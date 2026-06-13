---
hide:
  - navigation
  - toc
---

<div class="ragunit-hero" markdown>

![Baretto](assets/logo-baretto.png){ .hero-logo }

# RAGUnit

<p class="hero-tagline"><strong>The LLM evaluation library for the JVM — credible, auditable, local-first.</strong></p>

Judge your RAG pipelines, agents, and any LLM output straight from JUnit 5.
No Python. **Local-first** with Ollama — your data stays on your machine — or
plug any OpenAI-compatible API (OpenAI, Claude, Gemini, Groq…) when you want a
hosted frontier judge. Your call.

[Get Started](getting-started.md){ .md-button .md-button--primary }
[View on GitHub](https://github.com/baretto-labs/ragunit){ .md-button }

</div>

<div class="grid cards" markdown>

-   :material-eye-outline:{ .lg .middle } **Visible prompts**

    ---

    A score has no meaning if you can't read the question asked to the judge.
    Every prompt is a public, **versioned** constant — wording never changes silently.

    [:octicons-arrow-right-24: Custom Prompts](custom-prompts.md)

-   :material-chart-bell-curve:{ .lg .middle } **Variance under control**

    ---

    One judge call is noise. `withRuns(n)` averages N runs and fails when the
    judge is too unstable — the library owns reproducibility, not you.

    [:octicons-arrow-right-24: Variance Control](variance-control.md)

-   :material-text-search:{ .lg .middle } **Auditable verdicts**

    ---

    `JudgeResult` exposes the score, the justification, the **exact prompt sent**,
    and the raw LLM response. Failing tests explain themselves.

    [:octicons-arrow-right-24: Concepts](concepts.md)

-   :material-shape-outline:{ .lg .middle } **Not just RAG**

    ---

    `JudgeQuery` evaluates any (input / output / reference) triplet against an
    ad-hoc criterion: summaries, agent replies, generated SQL…

    [:octicons-arrow-right-24: Generic Evaluation](judge-query.md)

-   :material-server-network:{ .lg .middle } **Local-first, your choice**

    ---

    Run fully local with Ollama for regulated or sensitive data, or point a single
    `OpenAiCompatibleJudge` at OpenAI, Claude, Gemini, Groq, vLLM… — same API.

    [:octicons-arrow-right-24: Providers](providers.md)

-   :material-package-variant-closed:{ .lg .middle } **Zero-dependency core**

    ---

    `ragunit-core` pulls **no** production dependency. Pure Java 17, JDK HttpClient.
    Auditable supply chain by construction.

</div>

---

## Quick Start

RAGUnit is distributed via [JitPack](https://jitpack.io/#baretto-labs/ragunit):

=== "Maven"

    ```xml
    <repositories>
      <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
      </repository>
    </repositories>

    <dependency>
      <groupId>com.github.baretto-labs.ragunit</groupId>
      <artifactId>ragunit-core</artifactId>
      <version>v0.2.1</version>
      <scope>test</scope>
    </dependency>
    ```

=== "Gradle"

    ```groovy
    repositories {
        maven { url 'https://jitpack.io' }
    }

    dependencies {
        testImplementation 'com.github.baretto-labs.ragunit:ragunit-core:v0.2.1'
    }
    ```

Start a local model and write your first evaluation test:

```java
@RagTest
class ApiDocPipelineTest {

    RagJudge judge = OllamaJudge.builder().model("qwen2.5:14b").build();

    @Test
    void should_be_faithful_and_relevant() {
        Question q = new Question("What happens if I exceed the API rate limit?");
        List<Document> ctx = myRetriever.retrieve(q.text());
        Answer a = myGenerator.generate(q.text(), ctx);

        RagAssert.assertThatAnswer(a)
                 .contains("429")               // hard check — instant, zero cost
                 .givenContext(ctx)
                 .forQuestion(q)
                 .evaluatedBy(judge)
                 .withRuns(3)                   // mean of 3 runs + stability bound
                 .isFaithfulToContext(0.80)
                 .isRelevantToQuestion(0.75);
    }
}
```

Results are written to `target/ragunit-report.json` automatically.

---

## Why RAGUnit?

| | RAGUnit | RAGAS / DeepEval |
|---|---|---|
| **Language** | Java 17+ | Python |
| **Test runner** | JUnit 5 native | pytest / standalone |
| **Judge** | Local (Ollama) **or** any OpenAI-compatible API | Cloud API |
| **Data privacy** | Local-first — your choice | API calls |
| **EU AI Act** | Local mode keeps data in-house | Depends on model |
| **Dependencies** | **Zero** (core) | Many |
| **Judge prompts** | Public, versioned | Internal |

---

## Next Steps

- [Getting Started](getting-started.md) — step-by-step setup
- [Concepts](concepts.md) — the domain model, `Verdict`, `JudgeResult`
- [Metrics](metrics/index.md) — deep dives on each metric
- [Cookbook](cookbook.md) — production scenarios: regressions, retriever bugs, GDPR, release gates
- [Providers](providers.md) — local Ollama, any OpenAI-compatible API, custom adapters
