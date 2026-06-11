# Faithfulness

**Module**: `ragunit-core`
**Requires**: `Answer`, `Question`, `Context`, `RagJudge`
**Returns**: `Verdict`

---

## What it measures

Faithfulness evaluates whether every claim in the Answer is grounded in the retrieved Context.
A score of 1.0 means every statement is directly supported. A score of 0.0 means none are.

This is the primary hallucination detector in RAGUnit.

| Score | Interpretation |
|---|---|
| 0.9–1.0 | All claims supported — no hallucination |
| 0.7–0.9 | Most claims supported, minor gaps |
| 0.5–0.7 | Several unsupported claims |
| < 0.5 | Significant hallucination |

---

## Formula

The judge decomposes the Answer into atomic statements, then checks each against the Context:

```
faithfulness = supported_statements / total_statements
```

When the judge returns a structured list of `Statement` objects, `Verdict.computedScore()` recalculates
the score mechanically — independent of the LLM's declared score.

---

## Usage

```java
Question question = new Question("Who discovered penicillin?");
List<Document> context = List.of(
    new Document("Alexander Fleming discovered penicillin in 1928 at St Mary's Hospital.")
);
Answer answer = new Answer("Fleming discovered penicillin in 1928.");

RagJudge judge = new OllamaJudge("qwen2.5:14b");

RagAssert.assertThatAnswer(answer)
         .givenContext(context)
         .forQuestion(question)
         .evaluatedBy(judge)
         .isFaithfulToContext(0.80);
```

---

## When to use it

- Every RAG pipeline should evaluate faithfulness — it is the core hallucination signal
- Combine with [Factual Correctness](factual-correctness.md) to distinguish *context hallucination*
  (answer contradicts context) from *knowledge hallucination* (context itself was wrong)

---

## Minimum recommended model

`qwen2.5:14b` — statement decomposition requires strong instruction-following.
