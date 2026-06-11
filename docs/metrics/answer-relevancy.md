# Answer Relevancy

**Module**: `ragunit-core`
**Requires**: `Answer`, `Question`, `RagJudge`
**Returns**: `Verdict`

---

## What it measures

Answer Relevancy evaluates whether the Answer is on-topic for the Question — that it actually
addresses what was asked, rather than drifting to tangential information.

Unlike [Faithfulness](faithfulness.md), this metric does **not** check the Context.
It is a pure question-answer alignment signal.

| Score | Interpretation |
|---|---|
| 0.9–1.0 | Answer directly and completely addresses the question |
| 0.7–0.9 | Mostly on-topic with minor digressions |
| 0.5–0.7 | Partially addresses the question |
| < 0.5 | Off-topic or evasive answer |

---

## Approach

The judge uses a **hypothetical-question** approach:
it generates N hypothetical questions from the Answer, then measures how many are semantically
aligned with the original Question. Score = aligned / total.

This is the LLM-only variant. For an embedding-based version, use [Semantic Similarity](semantic-similarity.md).

---

## Usage

```java
Question question = new Question("What is the boiling point of water?");
Answer answer = new Answer("Water boils at 100°C at standard atmospheric pressure.");

RagJudge judge = new OllamaJudge("qwen2.5:14b");

RagAssert.assertThatAnswer(answer)
         .forQuestion(question)
         .evaluatedBy(judge)
         .isRelevantToQuestion(0.75);
```

---

## When to use it

- Detecting verbose or evasive generators that provide technically correct but unfocused answers
- Pairing with Faithfulness: a pipeline can be faithful (grounded in context) but irrelevant
  (answering a different question than asked)
