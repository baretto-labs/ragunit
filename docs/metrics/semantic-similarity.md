# Semantic Similarity

**Module**: `ragunit-embedding`
**Requires**: `Answer`, `ReferenceAnswer` or `Question`, `EmbeddingProvider`
**Returns**: cosine similarity score

---

## What it measures

Semantic Similarity computes the cosine similarity between two text embeddings.
Unlike LLM-as-judge metrics, this is a **pure vector operation** — fast, deterministic, and
independent of any generative model.

Two modes are available:

| Mode | API | Measures |
|---|---|---|
| **vs Reference** | `hasSemanticSimilarity(t)` | How close is the Answer to the ground-truth? |
| **vs Question** | `isRelevantToQuestion(t)` | How well does the Answer address the Question? |

---

## Dependency

Add `ragunit-embedding` to your project:

=== "Maven"

    ```xml
    <dependency>
      <groupId>org.ragunit</groupId>
      <artifactId>ragunit-embedding</artifactId>
      <version>0.1</version>
      <scope>test</scope>
    </dependency>
    ```

=== "Gradle"

    ```groovy
    testImplementation 'org.ragunit:ragunit-embedding:0.1'
    ```

Pull an embedding model:

```bash
ollama pull nomic-embed-text
```

---

## Usage

=== "Answer vs Reference"

    ```java
    Answer answer = new Answer("Fleming discovered penicillin in 1928.");
    ReferenceAnswer reference = new ReferenceAnswer(
        "Alexander Fleming discovered penicillin in 1928 at St Mary's Hospital.");

    EmbeddingProvider embedder = new OllamaEmbeddingProvider("nomic-embed-text");

    EmbeddingAssert.assertThatAnswer(answer)
                   .comparedTo(reference)
                   .using(embedder)
                   .hasSemanticSimilarity(0.85);
    ```

=== "Answer vs Question"

    ```java
    Answer answer = new Answer("Water boils at 100°C at sea level.");
    Question question = new Question("What is the boiling point of water?");

    EmbeddingAssert.assertThatAnswer(answer)
                   .forQuestion(question)
                   .using(embedder)
                   .isRelevantToQuestion(0.75);
    ```

---

## Score range

Cosine similarity is in `[-1.0, 1.0]`:

| Score | Interpretation |
|---|---|
| 0.9–1.0 | Nearly identical meaning |
| 0.7–0.9 | Very similar |
| 0.5–0.7 | Related topic |
| < 0.5 | Different meaning |

Typical thresholds: `0.80–0.90` for answer-vs-reference, `0.70–0.80` for answer-vs-question.

---

## When to use it

- Fast, deterministic checks in CI (no LLM call)
- Regression testing after model upgrades
- Complement to LLM-as-judge metrics when latency matters

---

## Recommended embedding model

`nomic-embed-text` — 768 dimensions, fast, accurate. Pull with:

```bash
ollama pull nomic-embed-text
```
