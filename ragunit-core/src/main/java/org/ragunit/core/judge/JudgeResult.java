package org.ragunit.core.judge;

import org.ragunit.core.domain.Verdict;

import java.util.Objects;

/**
 * The structured, inspection-friendly view of one judge evaluation: the raw score,
 * the judge's justification, the exact prompt that was sent, and the raw LLM response.
 *
 * <p>When a test fails, a number is not enough — this record gives the user
 * everything needed to audit the measurement: what was asked ({@code promptUsed}),
 * what the model answered ({@code rawResponse}), and why it scored that way
 * ({@code justification}).
 *
 * <p>Retrieve it from the assertion builders after any judged assertion:
 * <pre>{@code
 * AnswerAssert assertion = RagAssert.assertThatAnswer(answer)
 *         .givenContext(context).forQuestion(question).evaluatedBy(judge);
 * assertion.isFaithfulToContext(0.8);
 * JudgeResult result = assertion.lastJudgeResult().orElseThrow();
 * }</pre>
 *
 * <p>Note: {@code Result} is normally a forbidden name in RAGUnit (DOMAIN.md);
 * {@code JudgeResult} is the deliberate exception introduced in v0.2 as the
 * generic judge-output contract.
 *
 * @param score         the normalized quality score in [0.0, 1.0]
 * @param justification the judge's human-readable explanation of the score
 * @param promptUsed    the exact prompt sent to the judge LLM ("" if not captured)
 * @param rawResponse   the raw LLM response ("" if not captured)
 * @param model         the identifier of the model that produced this result
 */
public record JudgeResult(double score, String justification, String promptUsed,
                          String rawResponse, String model) {

    /** Validates all fields. */
    public JudgeResult {
        Objects.requireNonNull(justification, "justification");
        Objects.requireNonNull(promptUsed, "promptUsed");
        Objects.requireNonNull(rawResponse, "rawResponse");
        Objects.requireNonNull(model, "model");
    }

    /**
     * Builds a JudgeResult from a {@link Verdict}, mapping the rationale to
     * {@code justification} and defaulting missing exchange fields to {@code ""}.
     *
     * @param verdict the verdict to convert
     * @return the structured result view of the verdict
     */
    public static JudgeResult fromVerdict(Verdict verdict) {
        Objects.requireNonNull(verdict, "verdict");
        return new JudgeResult(verdict.score().value(), verdict.rationale(),
                verdict.promptUsed().orElse(""), verdict.rawResponse().orElse(""),
                verdict.model());
    }
}
