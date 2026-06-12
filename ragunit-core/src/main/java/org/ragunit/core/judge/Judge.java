package org.ragunit.core.judge;

/**
 * The generic LLM-as-judge contract: evaluate any {@link JudgeQuery} —
 * a {@link Criterion} plus arbitrary named inputs — and return a structured
 * {@link JudgeResult}.
 *
 * <p>This is the lowest-level entry point of RAGUnit. It is not RAG-shaped:
 * any (input / output / reference) triplet can be judged, e.g. a summary
 * against its source, an agent reply against a policy, a SQL answer against
 * an expected result.
 *
 * <pre>{@code
 * JudgeQuery query = JudgeQuery.builder()
 *         .criterion(Criterion.of("conciseness", "Is the summary concise and faithful?"))
 *         .input("Source", article)
 *         .input("Summary", summary)
 *         .build();
 *
 * JudgeResult result = judge.evaluate(query);
 * }</pre>
 *
 * <p>{@link RagJudge} extends this contract with RAG-specific typed methods
 * and dispatches built-in {@link MetricType} criteria to them automatically.
 */
public interface Judge {

    /**
     * Evaluates the given query and returns a structured result.
     *
     * @param query the criterion and named inputs to judge
     * @return the structured result: score, justification, prompt used, raw response
     */
    JudgeResult evaluate(JudgeQuery query);
}
