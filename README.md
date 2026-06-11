# RAGUnit

**The evaluation library for RAG pipelines — Java-native, 100% local, zero cloud dependency.**

RAGUnit integrates into your JUnit 5 test suite and evaluates your RAG pipeline using an LLM-as-judge.
No Python. No API key. No data leaves your infrastructure.

[![Build](https://github.com/your-org/ragunit/actions/workflows/ci.yml/badge.svg)](https://github.com/your-org/ragunit/actions/workflows/ci.yml)
[![Docs](https://github.com/your-org/ragunit/actions/workflows/docs.yml/badge.svg)](https://your-org.github.io/ragunit)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](LICENSE)

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
  <groupId>com.github.baretto-labs</groupId>
  <artifactId>ragunit</artifactId>
  <version>0.1</version>
  <scope>test</scope>
</dependency>
```

```bash
ollama pull qwen2.5:14b && ollama serve
```

```java
@RagTest
class MyPipelineTest {
    RagJudge judge = new OllamaJudge("qwen2.5:14b");

    @Test
    void answer_should_be_faithful() {
        RagAssert.assertThatAnswer(answer)
                 .givenContext(context)
                 .forQuestion(question)
                 .evaluatedBy(judge)
                 .isFaithfulToContext(0.80);
    }
}
```

**Full documentation**: [your-org.github.io/ragunit](https://your-org.github.io/ragunit)

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

## Integrations

`OllamaJudge` built-in — zero dependencies, 100% local. Implement `RagJudge` to use any other LLM framework.

## Contributing

See [CONTRIBUTING.md](docs/contributing.md) and [DOMAIN.md](DOMAIN.md).
