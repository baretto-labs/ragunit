# Custom Prompts

Domain-specific prompts improve metric accuracy by 10–20% (RAGAS research).
RAGUnit lets you replace the built-in prompt for any metric via `JudgePromptTemplate`.

---

## Why customize prompts?

The default prompts are tuned for general-purpose RAG pipelines. For specialized domains:

- **Legal**: strict citation standards, precise claim definitions
- **Medical**: clinical accuracy, evidence-based standards
- **Financial**: regulatory context, numerical precision
- **Code**: semantic equivalence rather than textual similarity

---

## JudgePromptTemplate

`JudgePromptTemplate` is a functional interface:

```java
@FunctionalInterface
public interface JudgePromptTemplate {
    String render(PromptContext ctx);
}
```

`PromptContext` provides:

```java
ctx.question()           // Question
ctx.retrievedContext()   // List<Document>
ctx.answer()             // Optional<Answer>
```

---

## Usage

```java
JudgePromptTemplate legalFaithfulness = ctx ->
    "You are a legal RAG evaluation judge. Apply strict citation standards.\n\n"
    + "ANSWER:\n" + ctx.answer().map(Answer::text).orElse("") + "\n\n"
    + "CONTEXT:\n" + ctx.retrievedContext().stream()
          .map(Document::content)
          .collect(Collectors.joining("\n---\n"))
    + "\n\nFor each claim in the ANSWER, state whether it is directly supported by "
    + "the CONTEXT. Then return:\n"
    + "{\"score\": <0.0-1.0>, \"rationale\": \"<explanation>\"}";

RagJudge judge = new OllamaJudge("qwen2.5:14b",
    Map.of(MetricType.GENERATION, legalFaithfulness));
```

---

## Overriding multiple metrics

```java
Map<MetricType, JudgePromptTemplate> templates = Map.of(
    MetricType.GENERATION, legalFaithfulness,
    MetricType.ANSWER_RELEVANCY, legalRelevancy,
    MetricType.CONTEXT_RECALL, legalRecall
);

RagJudge judge = new OllamaJudge("qwen2.5:14b", templates);
```

---

## MetricType enum

| `MetricType` | Metric |
|---|---|
| `RETRIEVAL` | Context relevance |
| `GENERATION` | Faithfulness |
| `ANSWER_RELEVANCY` | Answer relevancy |
| `CONTEXT_PRECISION` | Context precision |
| `CONTEXT_RECALL` | Context recall |
| `FACTUAL_CORRECTNESS` | Factual correctness |
| `REJECTION` | Rejection justification |
| `PROMPT_INJECTION` | Prompt injection safety |
| `PII_LEAK` | PII exposure |
| `TOOL_TRAJECTORY` | Agentic tool trajectory |

---

## Output format contract

Your custom prompt **must** instruct the model to return valid JSON with at least:

```json
{"score": 0.85, "rationale": "..."}
```

For `FACTUAL_CORRECTNESS`, also include `precision` and `recall`:

```json
{"score": 0.80, "precision": 0.90, "recall": 0.72, "rationale": "..."}
```

The `score` field is clamped to `[0.0, 1.0]` regardless of the model's output.
Missing `precision`/`recall` fields default to the `score` value.
