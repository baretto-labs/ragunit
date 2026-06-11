package org.ragunit.core.assertion;

import org.ragunit.core.domain.Answer;
import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.FactualCorrectnessVerdict;
import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.ReferenceAnswer;
import org.ragunit.core.domain.Score;
import org.ragunit.core.domain.ScoreStatistics;
import org.ragunit.core.domain.ToolCall;
import org.ragunit.core.domain.Verdict;
import org.ragunit.core.judge.RagJudge;
import org.ragunit.core.report.AssertionResult;
import org.ragunit.core.report.RagReporter;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Builder for the Generation evaluation flow.
 *
 * <p>Obtain an instance via {@link RagAssert#assertThatAnswer(Answer)}.
 * Each assertion method calls the judge independently — one LLM call per metric,
 * multiplied by {@link #withRuns(int)} when variance control is enabled.
 */
public final class AnswerAssert {

    private final Answer answer;
    private final List<RagReporter> reporters;
    private List<Document> context;
    private Question question;
    private RagJudge judge;
    private ReferenceAnswer reference;
    private int runs = 1;
    private double maxStddev = ScoreStatistics.DEFAULT_MAX_STDDEV;

    /**
     * Creates a new AnswerAssert for the given answer.
     *
     * @param answer    the answer to evaluate; must not be null
     * @param reporters observers notified after each assertion; must not be null
     */
    public AnswerAssert(Answer answer, List<RagReporter> reporters) {
        this.answer = Objects.requireNonNull(answer, "answer");
        this.reporters = Objects.requireNonNull(reporters, "reporters");
    }

    /**
     * Sets the context used by the Generator.
     *
     * @param context the retrieved documents; must not be null
     * @return this, for chaining
     */
    public AnswerAssert givenContext(List<Document> context) {
        this.context = List.copyOf(Objects.requireNonNull(context, "context"));
        return this;
    }

    /**
     * Sets the question the answer was generated for.
     *
     * @param question the original user question; must not be null
     * @return this, for chaining
     */
    public AnswerAssert forQuestion(Question question) {
        this.question = Objects.requireNonNull(question, "question");
        return this;
    }

    /**
     * Sets the ground-truth reference answer for FactualCorrectness evaluation.
     *
     * @param reference the human-authored reference answer
     * @return this, for chaining
     */
    public AnswerAssert comparedTo(ReferenceAnswer reference) {
        this.reference = Objects.requireNonNull(reference, "reference");
        return this;
    }

    /**
     * Sets the judge responsible for evaluating generation quality.
     *
     * @param judge the LLM-as-judge implementation; must not be null
     * @return this, for chaining
     */
    public AnswerAssert evaluatedBy(RagJudge judge) {
        this.judge = Objects.requireNonNull(judge, "judge");
        return this;
    }

    /**
     * Enables variance control: every subsequent assertion runs the judge
     * {@code runs} times and asserts on the mean score, failing additionally
     * when the standard deviation exceeds {@link #withMaxStddev(double)}.
     *
     * <p>A single LLM-judge call has ±10–15% variance; 3 to 5 runs turn a
     * noisy score into a defensible measurement.
     *
     * @param runs the number of judge calls per assertion (≥ 1)
     * @return this, for chaining
     * @throws IllegalArgumentException if {@code runs < 1}
     */
    public AnswerAssert withRuns(int runs) {
        if (runs < 1) {
            throw new IllegalArgumentException("runs must be >= 1, got " + runs);
        }
        this.runs = runs;
        return this;
    }

    /**
     * Sets the maximum acceptable standard deviation across runs
     * (default {@link ScoreStatistics#DEFAULT_MAX_STDDEV}).
     *
     * @param maxStddev the stability bound (≥ 0)
     * @return this, for chaining
     * @throws IllegalArgumentException if {@code maxStddev} is negative
     */
    public AnswerAssert withMaxStddev(double maxStddev) {
        if (maxStddev < 0) {
            throw new IllegalArgumentException("maxStddev must be >= 0, got " + maxStddev);
        }
        this.maxStddev = maxStddev;
        return this;
    }

    /**
     * Asserts that the answer is faithful to the given context.
     *
     * @param threshold minimum acceptable faithfulness score in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the mean score is below the threshold or the judge is unstable
     */
    public AnswerAssert isFaithfulToContext(double threshold) {
        requireJudgeAndQuestion();
        Objects.requireNonNull(context, "Call givenContext() before asserting");
        return assertMetric("FAITHFULNESS", "Faithfulness", threshold,
                () -> judge.evaluateGeneration(question, context, answer));
    }

    /**
     * Asserts that the answer is relevant to the original question using a
     * hypothetical-question approach (LLM-only, no embeddings required).
     *
     * <p>The judge generates hypothetical questions from the answer and measures
     * how many align with the original question. Score = aligned / total.
     *
     * @param threshold minimum acceptable relevancy score in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the mean score is below the threshold or the judge is unstable
     */
    public AnswerAssert isRelevantToQuestion(double threshold) {
        requireJudgeAndQuestion();
        return assertMetric("ANSWER_RELEVANCY", "Answer relevancy", threshold,
                () -> judge.evaluateAnswerRelevancy(question, answer));
    }

    /**
     * Asserts that the answer correctly indicates a refusal AND that refusal was
     * justified given the context and question.
     *
     * @param threshold minimum acceptable rejection-justification score in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the mean score is below the threshold or the judge is unstable
     */
    public AnswerAssert correctlyRefusedToAnswer(double threshold) {
        requireJudgeAndQuestion();
        Objects.requireNonNull(context, "Call givenContext() before asserting");
        return assertMetric("REJECTION", "Rejection justification", threshold,
                () -> judge.evaluateRejection(question, context, answer));
    }

    /**
     * Asserts that the sequence of tool calls is necessary, sufficient,
     * and in the correct order to answer the question.
     *
     * @param trajectory the ordered list of tool calls produced by the Generator
     * @param threshold  minimum acceptable trajectory quality score in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the mean score is below the threshold or the judge is unstable
     */
    public AnswerAssert hasValidToolTrajectory(List<ToolCall> trajectory, double threshold) {
        requireJudgeAndQuestion();
        Objects.requireNonNull(trajectory, "trajectory");
        return assertMetric("TOOL_TRAJECTORY", "Tool trajectory quality", threshold,
                () -> judge.evaluateToolTrajectory(question, trajectory, answer));
    }

    /**
     * Asserts that the generated answer contains no prompt injection content.
     *
     * @param threshold minimum acceptable safety score in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the mean score is below the threshold or the judge is unstable
     */
    public AnswerAssert isSafeFromPromptInjection(double threshold) {
        requireJudgeAndQuestion();
        Objects.requireNonNull(context, "Call givenContext() before asserting");
        return assertMetric("PROMPT_INJECTION", "Prompt injection safety", threshold,
                () -> judge.evaluatePromptInjection(question, context, answer));
    }

    /**
     * Asserts that the generated answer leaks no personally identifiable information.
     *
     * @param threshold minimum acceptable compliance score in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the mean score is below the threshold or the judge is unstable
     */
    public AnswerAssert hasNoPIILeak(double threshold) {
        requireJudgeAndQuestion();
        Objects.requireNonNull(context, "Call givenContext() before asserting");
        return assertMetric("PII_LEAK", "PII leak compliance", threshold,
                () -> judge.evaluatePIILeak(question, context, answer));
    }

    /**
     * Asserts that the answer's F1 factual correctness score meets the threshold.
     *
     * <p>F1 = harmonic mean of precision (Answer claims in Reference) and
     * recall (Reference claims in Answer). Requires {@link #comparedTo(ReferenceAnswer)}.
     *
     * @param threshold minimum acceptable F1 score in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the mean F1 is below the threshold or the judge is unstable
     */
    public AnswerAssert hasFactualCorrectnessF1(double threshold) {
        return assertFactualMetric("Factual correctness F1", threshold,
                FactualCorrectnessVerdict::f1);
    }

    /**
     * Asserts that the answer's factual precision meets the threshold.
     *
     * <p>Precision = fraction of Answer claims supported by the ReferenceAnswer.
     *
     * @param threshold minimum acceptable precision in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the mean precision is below the threshold or the judge is unstable
     */
    public AnswerAssert hasFactualCorrectnessPrecision(double threshold) {
        return assertFactualMetric("Factual correctness precision", threshold,
                FactualCorrectnessVerdict::precision);
    }

    /**
     * Asserts that the answer's factual recall meets the threshold.
     *
     * <p>Recall = fraction of ReferenceAnswer claims covered by the Answer.
     *
     * @param threshold minimum acceptable recall in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the mean recall is below the threshold or the judge is unstable
     */
    public AnswerAssert hasFactualCorrectnessRecall(double threshold) {
        return assertFactualMetric("Factual correctness recall", threshold,
                FactualCorrectnessVerdict::recall);
    }

    private AnswerAssert assertFactualMetric(String label, double threshold,
                                             Function<FactualCorrectnessVerdict, Score> selector) {
        requireJudgeAndQuestion();
        Objects.requireNonNull(reference, "Call comparedTo() before asserting factual correctness");
        return assertMetric("FACTUAL_CORRECTNESS", label, threshold, () -> {
            FactualCorrectnessVerdict verdict =
                    judge.evaluateFactualCorrectness(question, answer, reference);
            return Verdict.of(selector.apply(verdict), verdict.rationale(), verdict.model());
        });
    }

    private AnswerAssert assertMetric(String type, String label, double threshold,
                                      Supplier<Verdict> evaluation) {
        RepeatedEvaluation evaluated = RepeatedEvaluation.run(runs, evaluation);
        boolean passed = evaluated.passes(threshold, maxStddev);
        silentlyReport(new AssertionResult(type, question, evaluated.aggregatedVerdict(),
                threshold, passed));
        if (!passed) {
            throw new AssertionError(evaluated.failureMessage(label, threshold, maxStddev));
        }
        return this;
    }

    private void requireJudgeAndQuestion() {
        Objects.requireNonNull(judge, "Call evaluatedBy() before asserting");
        Objects.requireNonNull(question, "Call forQuestion() before asserting");
    }

    private void silentlyReport(AssertionResult result) {
        for (RagReporter reporter : reporters) {
            try {
                reporter.report(result);
            } catch (Exception ignored) {
                // Reporters must never interrupt test execution
            }
        }
    }
}
