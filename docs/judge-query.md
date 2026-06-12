# Generic Evaluation (JudgeQuery)

`RagAssert` is RAG-shaped: `forQuestion(...)`, `givenContext(...)`. But LLM
evaluation is not only about RAG. The generic entry point lets you judge **any**
(input / output / reference) triplet: a summary against its source, an agent reply
against a policy, a generated SQL answer against an expected result.

---

## The three pieces

| Type | Role |
|---|---|
| `Criterion` | The named evaluation question, e.g. *"Is the summary concise and faithful?"* |
| `JudgeQuery` | A criterion plus arbitrary named inputs, built via builder |
| `Judge` | The generic contract: `JudgeResult evaluate(JudgeQuery)` |

## Judging a non-RAG case

```java
Judge judge = OllamaJudge.builder().model("qwen2.5:14b").build();

JudgeQuery query = JudgeQuery.builder()
        .criterion(Criterion.of("summary-completeness",
                "Does the summary cover all key points of the source without adding facts?"))
        .input("Source", article)
        .input("Summary", summary)
        .build();

JudgeResult result = judge.evaluate(query);

result.score();          // 0.0 – 1.0
result.justification();  // why the judge scored it that way
result.promptUsed();     // the exact prompt that was sent — auditable
result.rawResponse();    // the raw LLM response
```

Inputs are rendered into the prompt in insertion order. Multi-valued inputs render
as bullet lists:

```java
.input("context", List.of("chunk one", "chunk two"))
```

The generic prompt is versioned: `JudgePromptLibrary.criterionPromptV1(query)`
shows exactly what will be sent.

## Custom criteria as enums

`Criterion` is an interface, so a project can catalogue its criteria:

```java
enum SupportBotCriteria implements Criterion {
    TONE("Is the reply professional and empathetic?"),
    POLICY("Does the reply comply with the refund policy?");

    private final String instruction;
    SupportBotCriteria(String instruction) { this.instruction = instruction; }
    @Override public String instruction() { return instruction; }
}
```

## Built-in metrics are criteria too

Every `MetricType` implements `Criterion`. A query whose criterion is a built-in
metric is dispatched to the judge's typed RAG method — honoring the per-metric
prompt templates — using the canonical input names:

```java
JudgeQuery query = JudgeQuery.builder()
        .criterion(MetricType.GENERATION)               // = faithfulness
        .input(JudgeQuery.INPUT_QUESTION, "…")
        .input(JudgeQuery.INPUT_CONTEXT, List.of("…"))
        .input(JudgeQuery.INPUT_ANSWER, "…")
        .build();
```

This dispatch is what `RagAssert` uses under the hood since v0.2 — the fluent
RAG API is now a thin layer over `JudgeQuery`. Custom `RagJudge` implementations
get query support for free; only arbitrary (non-metric) criteria require
overriding `verdictFor(JudgeQuery)`.

!!! note
    `TOOL_TRAJECTORY` is not query-dispatchable (a tool trajectory has no faithful
    flat-text encoding) — call `evaluateToolTrajectory(...)` directly.
