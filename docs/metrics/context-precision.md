# Context Precision

**Module**: `ragunit-core`
**Requires**: `Question`, `Context`, `RagJudge`
**Returns**: `Verdict` (with `ChunkVerdict` decomposition)

---

## What it measures

Context Precision evaluates the **ranking quality** of the Retriever — whether the most relevant
documents are returned first. A high-precision retriever puts relevant chunks at the top of the list
where they have the most influence on the Generator.

This is the RAGAS **Average Precision** metric.

| Score | Interpretation |
|---|---|
| 0.9–1.0 | All relevant chunks ranked first |
| 0.7–0.9 | Mostly well-ranked with minor misplacements |
| 0.5–0.7 | Relevant chunks scattered through the list |
| < 0.5 | Relevant chunks buried — retriever ranking is poor |

---

## Formula

Each chunk is evaluated independently (relevant or not), then Average Precision is computed:

```
AP = Σ (Precision@k × relevant_k) / total_relevant

where the sum is over positions k where the chunk at rank k is relevant.
```

This formula rewards pipelines that put relevant chunks early. A retriever that returns
all relevant chunks but ranks them last scores near 0.

The per-chunk breakdown is available in `Verdict.chunkVerdicts()`.

---

## Usage

```java
Question question = new Question("What are the rate limits per subscription plan?");
List<Document> context = List.of(
    new Document("Free: 100 req/min. Pro: 1000 req/min. Enterprise: unlimited."),  // rank 1 — relevant
    new Document("The SDK supports iOS 15+ and Android 8.0+ via CocoaPods/Gradle."), // rank 2 — irrelevant
    new Document("SLA guarantees 99.9% uptime. P1 response: 15 min.")              // rank 3 — irrelevant
);

RagJudge judge = new OllamaJudge("qwen2.5:14b");

RagAssert.assertThatContext(context)
         .forQuestion(question)
         .evaluatedBy(judge)
         .hasContextPrecision(0.75);
```

---

## When to use it

- Evaluating vector similarity search or BM25 ranking quality
- Diagnosing retriever degradation when answer quality drops
- Comparing retrieval strategies (e.g. naive cosine similarity vs. reranking)

---

## Difference from ContextRecall

| | Context Precision | Context Recall |
|---|---|---|
| **Measures** | Ranking quality | Coverage |
| **Requires** | Question + Context | Question + Context + ReferenceAnswer |
| **Question** | Are relevant chunks ranked first? | Are all key facts present? |
