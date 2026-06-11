# Getting Started

## Prerequisites

- Java 17+
- Maven 3.8+ or Gradle 7+
- [Ollama](https://ollama.ai) running locally

## 1. Install Ollama & pull a model

```bash
# Install Ollama (macOS)
brew install ollama

# Pull the recommended judge model
ollama pull qwen2.5:14b

# Start the server (runs on localhost:11434)
ollama serve
```

!!! tip "Minimum model size"
    `qwen2.5:14b` is the minimum recommended size for reliable claim decomposition.
    Smaller models (7B and below) produce inconsistent JSON and unreliable scores.

## 2. Add the dependency

RAGUnit is distributed via [JitPack](https://jitpack.io). Add the repository and the dependency:

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

## 3. Write your first test

```java
import org.junit.jupiter.api.Test;
import org.ragunit.core.RagTest;
import org.ragunit.core.assertion.RagAssert;
import org.ragunit.core.domain.*;
import org.ragunit.core.judge.OllamaJudge;
import org.ragunit.core.judge.RagJudge;

import java.util.List;

@RagTest // (1)
class MyRagPipelineTest {

    RagJudge judge = new OllamaJudge("qwen2.5:14b"); // (2)

    @Test
    void answer_should_be_faithful_to_context() {
        Question question = new Question("What is penicillin?");
        List<Document> context = List.of(
            new Document("Penicillin is an antibiotic discovered by Alexander Fleming in 1928.")
        );
        Answer answer = new Answer("Penicillin is an antibiotic discovered by Fleming.");

        RagAssert.assertThatAnswer(answer) // (3)
                 .givenContext(context)
                 .forQuestion(question)
                 .evaluatedBy(judge)
                 .isFaithfulToContext(0.80); // (4)
    }
}
```

1. `@RagTest` tags this test as an expensive evaluation test. It is excluded from fast CI runs but included when you run `mvn test -Dgroups=rag-eval`.
2. `OllamaJudge` calls your local Ollama server. No API key required.
3. The fluent API guides you through providing the required inputs for each metric.
4. The assertion throws `AssertionError` if the faithfulness score is below 0.80.

## 4. Run the test

```bash
# Run only @RagTest-tagged tests (requires Ollama running)
mvn test -Dgroups=rag-eval

# Run everything including @RagTest
mvn test -DexcludedGroups=

# Skip @RagTest in fast CI
mvn test   # @RagTest excluded by default
```

## 5. Read the report

Results are written to `target/ragunit-report.json` after each run:

```json
[
  {
    "assertionType": "FAITHFULNESS",
    "question": "What is penicillin?",
    "score": 0.92,
    "threshold": 0.80,
    "passed": true,
    "rationale": "All claims in the answer are directly supported by the context.",
    "model": "qwen2.5:14b"
  }
]
```

## Next Steps

- Add [more metrics](metrics/index.md) to your test
- Evaluate [retrieval quality](metrics/context-precision.md)
- Use [custom prompts](custom-prompts.md) for domain-specific accuracy
- Generate a [testset](testset-generation.md) from your documents
