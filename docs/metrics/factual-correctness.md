# Factual Correctness

**Module**: `ragunit-core`
**Requires**: `Answer`, `Question`, `ReferenceAnswer`, `RagJudge`
**Returns**: `FactualCorrectnessVerdict` (F1, precision, recall)

---

## What it measures

Factual Correctness evaluates whether the answer is **factually accurate** relative to a ground-truth
reference answer, at the level of individual claims.

It is orthogonal to **Faithfulness**: a pipeline can be faithful (no hallucination w.r.t. the retrieved
context) but still score low on Factual Correctness if the context itself contained incomplete or
incorrect information.

| Metric | Question answered |
|--------|------------------|
| **Precision** | Are the claims made in the Answer correct? (supported by the Reference) |
| **Recall** | Does the Answer cover the key facts from the Reference? |
| **F1** | Harmonic mean — balanced view of both precision and recall |

---

## Formula

The judge decomposes both the Answer and the ReferenceAnswer into atomic claims, then cross-checks them:

```
precision = (Answer claims supported by Reference) / (total Answer claims)
recall    = (Reference claims covered by Answer) / (total Reference claims)
F1        = 2 × precision × recall / (precision + recall)   [0 if both are 0]
```

---

## Usage

```java
Question question = new Question("Who discovered penicillin and when?");
Answer answer = new Answer("Alexander Fleming discovered penicillin in 1928.");
ReferenceAnswer reference = new ReferenceAnswer(
    "Alexander Fleming discovered penicillin in 1928 while working at St Mary's Hospital.");

RagJudge judge = new OllamaJudge("qwen2.5:14b");

// Assert on F1 (primary metric)
RagAssert.assertThatAnswer(answer)
    .forQuestion(question)
    .comparedTo(reference)
    .evaluatedBy(judge)
    .hasFactualCorrectnessF1(0.75);

// Assert on precision and recall independently
RagAssert.assertThatAnswer(answer)
    .forQuestion(question)
    .comparedTo(reference)
    .evaluatedBy(judge)
    .hasFactualCorrectnessPrecision(0.80)
    .hasFactualCorrectnessRecall(0.70);
```

---

## Interpreting scores

| Score range | Interpretation |
|-------------|---------------|
| 0.9 – 1.0 | Answer is highly accurate and complete |
| 0.7 – 0.9 | Good factual coverage, minor gaps or additions |
| 0.5 – 0.7 | Partial overlap — either missing key facts (low recall) or adding incorrect ones (low precision) |
| < 0.5 | Significant factual error or omission |

**Low precision, high recall**: the answer covers the reference but adds incorrect claims (hallucination-like).
**High precision, low recall**: the answer is accurate but misses important facts from the reference.

---

## When to use it

- Evaluating pipelines where **factual accuracy** against a known ground truth matters
- **Complement to Faithfulness**: use Faithfulness to detect context-level hallucination, use Factual Correctness to detect knowledge-level errors
- When your evaluation dataset includes **reference answers** (e.g. from testset generation)

---

## Minimum recommended model

`qwen2.5:14b` — claim decomposition requires strong instruction-following and reasoning ability.
Models smaller than 14B tend to produce unreliable claim-level breakdowns.

---

## Custom prompt

Override the default prompt for domain-specific claim extraction (e.g. legal, medical):

```java
JudgePromptTemplate legalTemplate = ctx ->
    "You are a legal RAG evaluation judge. Apply strict citation standards.\n\n"
    + "Answer: " + ctx.answer().map(a -> a.text()).orElse("") + "\n"
    + "Reference: ...\n"
    + "...";

RagJudge judge = new OllamaJudge("qwen2.5:14b",
    Map.of(MetricType.FACTUAL_CORRECTNESS, legalTemplate));
```
