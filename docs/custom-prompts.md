# Custom Prompts

Domain-specific prompts improve metric accuracy by 10–20% (RAGAS research).
RAGUnit lets you replace the built-in prompt for any metric via `JudgePromptTemplate`.

A score has no meaning if you cannot read the question asked to the judge —
so every built-in prompt is **public and versioned** in `JudgePromptLibrary`.

---

## Reading the built-in prompts

`JudgePromptLibrary` exposes every default prompt as a public, versioned constant
(`FAITHFULNESS_V1`, `ANSWER_RELEVANCY_V1`, …). To inspect the exact prompt a judge
sends for a metric:

```java
String prompt = JudgePromptLibrary.FAITHFULNESS_V1
        .render(PromptContext.forGeneration(question, context, answer));
System.out.println(prompt);
```

`JudgePromptLibrary.defaults()` returns the full `Map<MetricType, JudgePromptTemplate>`
a judge uses when nothing is overridden.

**Versioning contract**: prompt wording is never changed in place. A wording change
ships as a new constant (`_V2`) plus a CHANGELOG entry, so scores stay comparable
across library versions.

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
ctx.reference()          // Optional<ReferenceAnswer> — CONTEXT_RECALL, FACTUAL_CORRECTNESS
ctx.trajectory()         // List<ToolCall>            — TOOL_TRAJECTORY
```

---

## Usage — builder (recommended)

```java
JudgePromptTemplate legalFaithfulness = ctx -> """
        You are a legal RAG evaluation judge. Apply strict citation standards.

        ANSWER:
        %s

        CONTEXT:
        %s

        For each claim in the ANSWER, state whether it is directly supported by the CONTEXT.
        Then return: {"score": <0.0-1.0>, "rationale": "<explanation>"}
        """.formatted(
            ctx.answer().map(Answer::text).orElse(""),
            ctx.retrievedContext().stream()
                    .map(Document::content)
                    .collect(Collectors.joining("\n---\n")));

RagJudge judge = OllamaJudge.builder()
        .model("qwen2.5:14b")
        .faithfulnessPrompt(legalFaithfulness)
        .build();
```

Named builder shortcuts: `faithfulnessPrompt(...)`, `answerRelevancyPrompt(...)`,
`factualCorrectnessPrompt(...)`. Any other metric goes through the generic form:

```java
OllamaJudge judge = OllamaJudge.builder()
        .model("qwen2.5:14b")
        .prompt(MetricType.CONTEXT_RECALL, legalRecall)
        .prompt(MetricType.REJECTION, legalRejection)
        .build();
```

## Usage — constructor (still supported)

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

| `MetricType` | Metric | Default prompt |
|---|---|---|
| `RETRIEVAL` | Context relevance | `RETRIEVAL_V1` |
| `GENERATION` | Faithfulness | `FAITHFULNESS_V1` |
| `ANSWER_RELEVANCY` | Answer relevancy | `ANSWER_RELEVANCY_V1` |
| `CONTEXT_PRECISION` | Context precision | `CONTEXT_PRECISION_V1` |
| `CONTEXT_RECALL` | Context recall | `CONTEXT_RECALL_V1` |
| `FACTUAL_CORRECTNESS` | Factual correctness | `FACTUAL_CORRECTNESS_V1` |
| `CONTEXT_REJECTION` | Context insufficiency | `CONTEXT_REJECTION_V1` |
| `REJECTION` | Rejection justification | `REJECTION_V1` |
| `CONTEXT_PROMPT_INJECTION` | Injection in context | `CONTEXT_PROMPT_INJECTION_V1` |
| `PROMPT_INJECTION` | Injection in answer | `PROMPT_INJECTION_V1` |
| `CONTEXT_PII_LEAK` | PII in context | `CONTEXT_PII_LEAK_V1` |
| `PII_LEAK` | PII in answer | `PII_LEAK_V1` |
| `TOOL_TRAJECTORY` | Agentic tool trajectory | `TOOL_TRAJECTORY_V1` |

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
