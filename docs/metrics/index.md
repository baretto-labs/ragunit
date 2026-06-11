# Metrics Overview

RAGUnit evaluates RAG pipelines along two axes: **Retrieval** and **Generation**.

---

## Retrieval Metrics

These metrics evaluate the Retriever — the component that fetches `Context` from a knowledge base.

| Metric | Requires | Key Question |
|---|---|---|
| [Relevance](faithfulness.md) | Question, Context | Is the context relevant to the question? |
| [Context Precision](context-precision.md) | Question, Context | Are relevant chunks ranked first? |
| [Context Recall](context-recall.md) | Question, Context, ReferenceAnswer | Does context cover the reference facts? |

---

## Generation Metrics

These metrics evaluate the Generator — the LLM that produces an `Answer` from the `Context`.

| Metric | Requires | Key Question |
|---|---|---|
| [Faithfulness](faithfulness.md) | Question, Context, Answer | Is the answer grounded in the context? |
| [Answer Relevancy](answer-relevancy.md) | Question, Answer | Is the answer on-topic? |
| [Factual Correctness](factual-correctness.md) | Question, Answer, ReferenceAnswer | Are the answer's claims accurate? |
| [Rejection](rejection.md) | Question, Context, Answer | Was the refusal justified? |
| [Prompt Injection](prompt-injection.md) | Question, Context, Answer | Is the answer free of injected instructions? |
| [PII Leak](pii-leak.md) | Question, Context, Answer | Does the answer expose personal data? |

---

## Embedding Metrics (optional)

These metrics use vector embeddings instead of an LLM judge. Requires `ragunit-embedding`.

| Metric | Requires | Key Question |
|---|---|---|
| [Semantic Similarity](semantic-similarity.md) | Answer, ReferenceAnswer, EmbeddingProvider | How semantically close is the answer to the reference? |

---

## Choosing Thresholds

All metrics return a score in `[0.0, 1.0]`. Common starting points:

| Score | Interpretation |
|---|---|
| ≥ 0.90 | Production-ready |
| 0.75–0.90 | Good, minor gaps |
| 0.50–0.75 | Needs improvement |
| < 0.50 | Significant issues |

Start conservative (0.70–0.75) and tighten thresholds as your pipeline matures.

---

## Reference-free vs Reference-required

Most RAGUnit metrics are **reference-free** — they evaluate the pipeline's own outputs without a ground-truth answer:

- Faithfulness, AnswerRelevancy, ContextPrecision, Rejection, PromptInjection, PIILeak

Two metrics require a **ReferenceAnswer**:

- **ContextRecall** — needs ground truth to check if context covers all key facts
- **FactualCorrectness** — needs ground truth to verify claim accuracy
