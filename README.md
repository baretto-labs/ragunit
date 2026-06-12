# RAGUnit

**The LLM evaluation library for the JVM — Java-native, 100% local, zero cloud dependency.**

RAGUnit integrates into your JUnit 5 test suite and evaluates your LLM pipeline using an LLM-as-judge.
No Python. No API key. No data leaves your infrastructure.

[![Build](https://github.com/baretto-labs/ragunit/actions/workflows/ci.yml/badge.svg)](https://github.com/baretto-labs/ragunit/actions/workflows/ci.yml)
[![JitPack](https://jitpack.io/v/baretto-labs/ragunit.svg)](https://jitpack.io/#baretto-labs/ragunit)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](LICENSE)

A score you cannot audit is not a measurement. RAGUnit v0.2 is built on four guarantees:

1. **Visible prompts** — every judge prompt is a public, versioned constant
   ([`JudgePromptLibrary`](ragunit-core/src/main/java/org/ragunit/core/judge/JudgePromptLibrary.java)).
   Wording never changes silently; a change ships as a new `_V2` constant and a CHANGELOG entry.
2. **Variance under control** — `withRuns(n)` averages N judge runs and fails when the judge
   is too unstable (`stddev > 0.15` by default). Temperature 0 by default.
3. **Structured results** — `JudgeResult` exposes the score, the judge's justification,
   the exact prompt sent, and the raw LLM response. Failure messages include the justification.
4. **Not just RAG** — `Judge.evaluate(JudgeQuery)` judges any (input / output / reference)
   triplet with an ad-hoc `Criterion`: summaries, agent replies, generated SQL…

---

## Quick Start

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

```bash
ollama pull qwen2.5:14b && ollama serve
```

```java
@RagTest
class MyPipelineTest {
    RagJudge judge = OllamaJudge.builder().model("qwen2.5:14b").build();

    @Test
    void answer_should_be_faithful() {
        RagAssert.assertThatAnswer(answer)
                 .contains("/auth/token")        // hard check — instant, zero cost
                 .givenContext(context)
                 .forQuestion(question)
                 .evaluatedBy(judge)
                 .withRuns(3)                    // mean of 3 runs + stability bound
                 .isFaithfulToContext(0.80);     // soft judgment — LLM-as-judge
    }
}
```

Beyond RAG — judge anything against a criterion:

```java
JudgeQuery query = JudgeQuery.builder()
        .criterion(Criterion.of("summary-completeness",
                "Does the summary cover all key points of the source without adding facts?"))
        .input("Source", article)
        .input("Summary", summary)
        .build();

JudgeResult result = judge.evaluate(query);
result.promptUsed();      // audit exactly what was asked
result.justification();   // why this score
```

**Full documentation**: [ragunit.org](https://ragunit.org)

---

## Metrics

| Assertion | Metric |
|---|---|
| `isFaithfulToContext(t)` | Faithfulness |
| `isRelevantToQuestion(t)` | Answer Relevancy |
| `hasFactualCorrectnessF1(t)` | Factual Correctness |
| `hasContextPrecision(t)` | Context Precision |
| `hasContextRecall(ref, t)` | Context Recall |
| `correctlyRefusedToAnswer(t)` | Rejection |
| `isSafeFromPromptInjection(t)` | Prompt Injection |
| `hasNoPIILeak(t)` | PII Leak |
| `contains / containsAll / matches / hasMinLength` | Deterministic (no LLM call) |

## Supply chain

`ragunit-core` has **zero production dependencies** — consumers pull no transitive
dependency at all. Pure Java 17, JDK `HttpClient` only.

## Integrations

`OllamaJudge` built-in — zero dependencies, 100% local. Implement `RagJudge`
(or just `Judge`) to plug any other LLM framework — about 50 lines.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) and [DOMAIN.md](DOMAIN.md).
