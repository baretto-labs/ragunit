# Cookbook

Quatre scénarios réalistes qui montrent pourquoi les tests RAG sont indispensables en production.
Chaque scénario correspond à un problème concret que les équipes rencontrent sans le voir venir.

---

## Scenario 1 — Catching a regression after an LLM upgrade

**The situation**: your RAG pipeline works fine on `qwen2.5:7b`. You upgrade to `qwen2.5:14b`
for better quality. Three weeks later, a user reports that the chatbot is giving wrong information
about token expiry. Your manual QA didn't catch it.

**The root cause**: the larger model is more confident — and confidently wrong on edge cases.
It now synthesizes information across documents in ways that introduce false claims.

**How RAGUnit catches it**:

```java
/**
 * Regression test — run this on every model or prompt change.
 * If it turns red, investigate before releasing.
 */
@Test
@RagTest
void should_not_hallucinate_token_expiry_on_enterprise_plan() {
    var question = new Question("How long are API tokens valid on the Enterprise plan?");
    var context = List.of(new Document(
            "Access tokens expire after 3600 seconds on all plans. " +
            "Refresh tokens are valid for 30 days. " +
            "Use POST /auth/token/refresh to renew an expired token."));

    // The model may (incorrectly) infer "Enterprise = no limits = tokens never expire".
    // If it does, faithfulness score drops below 0.40.
    var hallucinatedAnswer = new Answer(
            "On the Enterprise plan, tokens never expire and remain permanently valid, " +
            "eliminating the need for token refresh.");

    Verdict verdict = judge.evaluateGeneration(question, context, hallucinatedAnswer);

    assertThat(verdict.score().value())
            .as("Faithfulness score should be low — 'tokens never expire' is not in the docs")
            .isLessThan(0.40);
}
```

!!! tip "Pattern: lock your baseline"
    Pin this test to a **specific, deterministic input** — not a live LLM call. You are testing
    whether the *judge* correctly evaluates a known-bad answer, not whether the *generator* hallucinates.
    Separate concerns: one test suite for generator quality, one for regression on known failure cases.

---

## Scenario 2 — The silent retriever bug

**The situation**: your vector store is re-indexed after a schema migration. The embedding
model hasn't changed, but the chunk boundaries shifted. Retrieval precision silently drops —
the right document is still retrieved, but ranked 3rd instead of 1st. The LLM is smart enough
to still produce a reasonable answer, so nothing looks obviously broken.

**Why it matters**: if your retriever degrades further, or if you add a re-ranker, the 3rd-ranked
document may be cut off entirely. The bug is invisible until it becomes catastrophic.

**RAGUnit tests both layers independently**:

```java
@Test
@RagTest
void should_rank_rate_limit_doc_before_noise() {
    var question = new Question("What is the rate limit for the Pro plan?");

    // Context as your retriever currently returns it — rate limit doc is at rank 1.
    // After the re-indexing bug it would slide to rank 3.
    var context = List.of(
            new Document("Pro plan: 1000 req/min. Enterprise: unlimited. HTTP 429 on exceed."), // rank 1
            new Document("The SDK supports iOS 15+ via CocoaPods and Android 8+ via Gradle."),  // rank 2
            new Document("Webhook deliveries are retried up to 5 times on failure.")            // rank 3
    );

    // Retrieval quality: is the relevant chunk ranked first?
    RagAssert.assertThatContext(context)
             .forQuestion(question)
             .evaluatedBy(judge)
             .hasContextPrecision(0.75);   // (1)
}

@Test
@RagTest
void should_generate_faithful_answer_from_rate_limit_context() {
    var question = new Question("What is the rate limit for the Pro plan?");
    var context = List.of(
            new Document("Pro plan: 1000 req/min. Enterprise: unlimited. HTTP 429 on exceed.")
    );
    var answer = myRagPipeline.generate(question, context);

    // Generation quality: is the answer grounded in the context?
    RagAssert.assertThatAnswer(answer)
             .givenContext(context)
             .forQuestion(question)
             .evaluatedBy(judge)
             .isFaithfulToContext(0.85);   // (2)
}
```

1. `hasContextPrecision` fails as soon as the relevant chunk slips in ranking — **before** the
   generated answer degrades. It catches the retriever bug at the root.
2. `isFaithfulToContext` catches generator bugs independently, so you know which layer broke.

!!! warning "If you only test the final answer"
    A faithful answer from a poorly ranked context masks the retriever bug entirely.
    By the time the answer quality drops, you may have already cut off key documents.
    Test retrieval and generation as **separate quality gates**.

---

## Scenario 3 — GDPR compliance as a CI check

**The situation**: your HR chatbot answers employee questions about company policies.
The knowledge base includes anonymized documents, but a few chunks accidentally contain
real personal data (a name, an email, an internal IP) left over from a data export.
Your generator echoes this PII verbatim in its answers.

**The cost of missing this**: a GDPR incident, a €20M fine, and a front-page story.
Manual review of every answer at scale is not realistic.

**Make it a gate, not a manual check**:

```java
/**
 * GDPR compliance gate — blocks release if the pipeline leaks PII.
 *
 * Run this suite against your full testset before every production deploy.
 * A single failure is enough to block the release.
 */
@Test
@RagTest
void should_not_expose_employee_personal_data_in_answer() {
    var question = new Question("What is the company's remote work policy?");

    // Simulates a context chunk that accidentally contains PII from a data export.
    var context = List.of(
            new Document("Remote work is allowed up to 3 days per week for all employees."),
            new Document(
                    // This chunk slipped through the anonymization pipeline:
                    "Policy approved by Marie Dupont (marie.dupont@corp.example, ext. 4821) " +
                    "on 2024-03-15. Internal ref: HR-2024-0042.")
    );

    // Simulate what the LLM produces when it echoes the contaminated context:
    var leakingAnswer = new Answer(
            "The remote work policy allows up to 3 days per week. " +
            "It was approved by Marie Dupont (marie.dupont@corp.example) on 2024-03-15.");

    Verdict verdict = judge.evaluatePIILeak(question, context, leakingAnswer);

    // Score close to 0 = PII detected in the answer. Test PASSES when PII is found.
    assertThat(verdict.score().value())
            .as("Answer leaks employee name and email — PII leak score should be low")
            .isLessThan(0.30);

    // Optional: log the rationale for the audit trail
    System.out.println("PII leak rationale: " + verdict.rationale());
}
```

!!! info "Inverting the assertion"
    `evaluatePIILeak` returns a score close to **0** when PII is detected (unsafe).
    The assertion checks that the score IS low — meaning the judge correctly detected the leak.
    This test *passes* when PII is found, acting as a detection test for known-bad content.

---

## Scenario 4 — Persistent quality gate across releases

**The situation**: you ship a new version of your RAG pipeline every two weeks. You need to
ensure that each release does not degrade answer quality compared to the previous one.
Manual evaluation is too slow; evaluating every user query is too expensive.

**The solution**: generate a representative testset from your documents once, persist it,
and run it as part of your release pipeline.

```java
/**
 * Quality gate for release v2.4.
 *
 * Step 1 (done once): generate a testset and export it.
 * Step 2 (every release): import and evaluate — fails if quality drops.
 */
@Test
@RagTest
void should_maintain_quality_gate_across_releases() throws Exception {
    // Load the frozen testset committed to the repository
    TestsetImporter importer = new JsonTestsetImporter();
    Testset testset = importer.load(Path.of("testsets/api-doc-v1.json"));

    RagJudge judge = new OllamaJudge("qwen2.5:14b");
    int failedCases = 0;

    for (TestCase tc : testset.cases()) {
        // Run your real pipeline — retriever + generator
        Answer answer = myRagPipeline.generate(tc.question(), tc.context());

        try {
            RagAssert.assertThatAnswer(answer)
                     .givenContext(tc.context())
                     .forQuestion(tc.question())
                     .comparedTo(tc.referenceAnswer())
                     .evaluatedBy(judge)
                     .isFaithfulToContext(0.75)     // no hallucination
                     .hasFactualCorrectnessF1(0.70); // no dropped facts
        } catch (AssertionError e) {
            failedCases++;
            System.err.println("FAILED: " + tc.question().text());
            System.err.println("  " + e.getMessage());
        }
    }

    // Allow at most 10% degradation across the testset
    double failureRate = (double) failedCases / testset.size();
    assertThat(failureRate)
            .as("More than 10%% of testset cases regressed in this release")
            .isLessThanOrEqualTo(0.10);
}
```

**Generating the testset (one-time setup)**:

```java
@Test
@RagTest
void generate_and_persist_testset() throws Exception {
    List<Document> corpus = loadApiDocumentation(); // your real corpus

    TestsetGenerator generator = new OllamaTestsetGenerator("qwen2.5:14b");
    Testset testset = generator.generate(corpus, 50); // 50 representative cases

    TestsetExporter exporter = new JsonTestsetExporter();
    exporter.export(testset, Path.of("testsets/api-doc-v1.json"));

    // Commit testsets/api-doc-v1.json to your repository.
    // It becomes the baseline for all future release evaluations.
}
```

!!! tip "Using the testset in CI"
    Commit the generated testset to your repository. In CI, set the release pipeline to run
    `mvn test -Dgroups=rag-eval` only on the `release/*` branch. Fast branches run unit tests
    only — `@RagTest` is excluded by default.

    ```yaml
    # .github/workflows/release.yml
    - name: RAG quality gate
      run: mvn test -Dgroups=rag-eval
      env:
        OLLAMA_HOST: ${{ secrets.OLLAMA_HOST }}
    ```

---

## What these scenarios have in common

| Scenario | What breaks silently without RAGUnit |
|---|---|
| LLM upgrade | Model hallucinates on edge cases — users notice before you do |
| Retriever re-indexing | Ranking degrades — answer quality drops weeks later |
| PII in knowledge base | GDPR leak ships to production undetected |
| New release | Quality regression discovered by users, not your team |

The common thread: **the failure is invisible at the code level**. No exception is thrown.
No compilation error. The pipeline "works" — it just produces wrong, unsafe, or degraded output.

RAGUnit makes these invisible failures visible, measurable, and blocking.
