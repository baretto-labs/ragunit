# RAGUnit

**The evaluation library for RAG pipelines — Java-native, 100% local, zero cloud dependency.**

RAGUnit integrates directly into your JUnit 5 test suite and evaluates your RAG pipeline using an LLM-as-judge.
No Python. No API key. No data leaves your infrastructure.

---

## Why RAGUnit?

| | RAGUnit | RAGAS / DeepEval |
|---|---|---|
| **Language** | Java 17+ | Python |
| **Test runner** | JUnit 5 native | pytest / standalone |
| **LLM required** | Local (Ollama) | Cloud API |
| **Data privacy** | 100% local | API calls |
| **EU AI Act** | Compliant | Depends on model |
| **Dependencies** | Zero (core) | Many |

---

## Quick Start

RAGUnit is distributed via [JitPack](https://jitpack.io):

=== "Maven"

    ```xml
    <repositories>
      <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
      </repository>
    </repositories>

    <dependency>
      <groupId>com.github.baretto-labs</groupId>
      <artifactId>ragunit</artifactId>
      <version>0.1</version>
      <scope>test</scope>
    </dependency>
    ```

=== "Gradle"

    ```groovy
    repositories {
        maven { url 'https://jitpack.io' }
    }

    dependencies {
        testImplementation 'com.github.baretto-labs:ragunit:0.1'
    }
    ```

Start a local model and write your first evaluation test:

```java
@RagTest
class ApiDocPipelineTest {

    RagJudge judge = new OllamaJudge("qwen2.5:14b");

    @Test
    void should_be_faithful_and_relevant() {
        Question q = new Question("What happens if I exceed the API rate limit?");
        List<Document> ctx = myRetriever.retrieve(q.text());
        Answer a = myGenerator.generate(q.text(), ctx);

        RagAssert.assertThatAnswer(a)
                 .givenContext(ctx)
                 .forQuestion(q)
                 .evaluatedBy(judge)
                 .isFaithfulToContext(0.80)
                 .isRelevantToQuestion(0.75);
    }
}
```

Results are written to `target/ragunit-report.json` automatically.

---

## Available Metrics

### Generation

| Assertion | Metric |
|-----------|--------|
| `isFaithfulToContext(t)` | [Faithfulness](metrics/faithfulness.md) |
| `isRelevantToQuestion(t)` | [Answer Relevancy](metrics/answer-relevancy.md) |
| `correctlyRefusedToAnswer(t)` | [Rejection](metrics/rejection.md) |
| `isSafeFromPromptInjection(t)` | [Prompt Injection](metrics/prompt-injection.md) |
| `hasNoPIILeak(t)` | [PII Leak](metrics/pii-leak.md) |
| `hasFactualCorrectnessF1(t)` | [Factual Correctness](metrics/factual-correctness.md) |

### Retrieval

| Assertion | Metric |
|-----------|--------|
| `hasRelevanceScore(t)` | [Relevance](metrics/faithfulness.md) |
| `hasContextPrecision(t)` | [Context Precision](metrics/context-precision.md) |
| `hasContextRecall(ref, t)` | [Context Recall](metrics/context-recall.md) |

### Embedding-based (optional)

| Assertion | Metric |
|-----------|--------|
| `hasSemanticSimilarity(t)` | [Semantic Similarity](metrics/semantic-similarity.md) |

---

## Next Steps

- [Getting Started](getting-started.md) — step-by-step setup
- [Concepts](concepts.md) — understand the domain model
- [Metrics](metrics/index.md) — deep dives on each metric
- [Cookbook](cookbook.md) — four production scenarios: regressions, retriever bugs, GDPR, release gates
- [Integrations](integrations/index.md) — Ollama setup and custom adapters
