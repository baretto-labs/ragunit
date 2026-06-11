# Context Recall

**Module**: `ragunit-core`
**Requires**: `Question`, `Context`, `ReferenceAnswer`, `RagJudge`
**Returns**: `Verdict`

---

## What it measures

Context Recall evaluates whether the retrieved Context contains all the information needed
to reconstruct the ReferenceAnswer. It decomposes the reference into atomic claims and
checks how many are supported by the Context.

```
recall = covered_reference_claims / total_reference_claims
```

| Score | Interpretation |
|---|---|
| 0.9–1.0 | Context contains virtually all reference facts |
| 0.7–0.9 | Most facts present, minor gaps |
| 0.5–0.7 | Significant coverage gaps — retriever is missing key documents |
| < 0.5 | Retriever is fundamentally missing relevant sources |

---

## Usage

```java
Question question = new Question("Who discovered penicillin and when?");
List<Document> context = List.of(
    new Document("Alexander Fleming discovered penicillin in 1928 at St Mary's Hospital in London.")
);
ReferenceAnswer reference = new ReferenceAnswer(
    "Alexander Fleming discovered penicillin in 1928.");

RagJudge judge = new OllamaJudge("qwen2.5:14b");

RagAssert.assertThatContext(context)
         .forQuestion(question)
         .evaluatedBy(judge)
         .hasContextRecall(reference, 0.80);
```

---

## When to use it

- When your evaluation dataset includes reference answers (e.g. from testset generation)
- Diagnosing retriever coverage problems: low recall means the retriever fails to find
  relevant documents, regardless of ranking
- Pairing with Context Precision to get a full retrieval quality picture:
  precision = ranking quality, recall = coverage

---

## This is the only retrieval metric requiring ground truth

All other RAGUnit metrics are reference-free. Context Recall requires a `ReferenceAnswer`
because coverage is defined relative to a known correct answer.

---

## Minimum recommended model

`qwen2.5:14b` — claim decomposition requires strong instruction-following.
