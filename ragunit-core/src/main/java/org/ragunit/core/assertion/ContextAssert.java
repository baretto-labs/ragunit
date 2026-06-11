package org.ragunit.core.assertion;

import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.ReferenceAnswer;
import org.ragunit.core.domain.ScoreStatistics;
import org.ragunit.core.domain.Verdict;
import org.ragunit.core.judge.RagJudge;
import org.ragunit.core.report.AssertionResult;
import org.ragunit.core.report.RagReporter;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Builder for the Retrieval evaluation flow.
 *
 * <p>Obtain an instance via {@link RagAssert#assertThatContext(List)}.
 * Each method returns {@code this} for chaining. Assertion methods trigger the Judge
 * (once, or {@link #withRuns(int)} times) and throw {@link AssertionError} when the
 * mean score is below the threshold or the judge is too unstable.
 */
public final class ContextAssert {

    private final List<Document> context;
    private final List<RagReporter> reporters;
    private Question question;
    private RagJudge judge;
    private int runs = 1;
    private double maxStddev = ScoreStatistics.DEFAULT_MAX_STDDEV;

    /**
     * Creates a new ContextAssert for the given retrieved documents.
     *
     * @param context   the retrieved documents to evaluate; must not be null
     * @param reporters observers notified after each assertion; must not be null
     */
    public ContextAssert(List<Document> context, List<RagReporter> reporters) {
        this.context = List.copyOf(Objects.requireNonNull(context, "context"));
        this.reporters = Objects.requireNonNull(reporters, "reporters");
    }

    /**
     * Sets the question the context was retrieved for.
     *
     * @param question the original user question; must not be null
     * @return this, for chaining
     */
    public ContextAssert forQuestion(Question question) {
        this.question = Objects.requireNonNull(question, "question");
        return this;
    }

    /**
     * Sets the judge responsible for evaluating retrieval quality.
     *
     * @param judge the LLM-as-judge implementation; must not be null
     * @return this, for chaining
     */
    public ContextAssert evaluatedBy(RagJudge judge) {
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
    public ContextAssert withRuns(int runs) {
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
    public ContextAssert withMaxStddev(double maxStddev) {
        if (maxStddev < 0) {
            throw new IllegalArgumentException("maxStddev must be >= 0, got " + maxStddev);
        }
        this.maxStddev = maxStddev;
        return this;
    }

    /**
     * Asserts that the context relevance score meets the given threshold.
     * Notifies reporters whether the assertion passes or fails.
     *
     * @param threshold minimum acceptable relevance score in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the mean score is below the threshold or the judge is unstable
     */
    public ContextAssert hasRelevanceScore(double threshold) {
        requireJudgeAndQuestion();
        return assertMetric("CONTEXT_RELEVANCE", "Context relevance", threshold,
                () -> judge.evaluateRetrieval(question, context));
    }

    /**
     * Asserts that the context was too insufficient to answer the question,
     * i.e. a refusal by the generator was justified.
     *
     * @param threshold minimum acceptable rejection-justification score in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the mean score is below the threshold or the judge is unstable
     */
    public ContextAssert correctlyRefusedToAnswer(double threshold) {
        requireJudgeAndQuestion();
        return assertMetric("REJECTION", "Rejection justification", threshold,
                () -> judge.evaluateContextRejection(question, context));
    }

    /**
     * Asserts that the retrieved context covers enough claims from the reference answer
     * (ground truth). Score = covered claims / total reference claims.
     *
     * <p>This is the only assertion requiring a ground truth — it measures whether the
     * Retriever found all the documents necessary to reconstruct the reference answer.
     *
     * @param reference the ground-truth answer to compare coverage against
     * @param threshold minimum acceptable recall score in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the mean score is below the threshold or the judge is unstable
     */
    public ContextAssert hasContextRecall(ReferenceAnswer reference, double threshold) {
        requireJudgeAndQuestion();
        Objects.requireNonNull(reference, "reference");
        return assertMetric("CONTEXT_RECALL", "Context recall", threshold,
                () -> judge.evaluateContextRecall(question, context, reference));
    }

    /**
     * Asserts that the context precision score (RAGAS Average Precision) meets the threshold.
     * Evaluates each retrieved chunk independently and rewards top-ranked relevant chunks.
     *
     * @param threshold minimum acceptable precision score in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the mean score is below the threshold or the judge is unstable
     */
    public ContextAssert hasContextPrecision(double threshold) {
        requireJudgeAndQuestion();
        return assertMetric("CONTEXT_PRECISION", "Context precision", threshold,
                () -> judge.evaluateContextPrecision(question, context));
    }

    /**
     * Asserts that the retrieved context contains no prompt injection attempts.
     *
     * @param threshold minimum acceptable safety score in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the mean score is below the threshold or the judge is unstable
     */
    public ContextAssert isSafeFromPromptInjection(double threshold) {
        requireJudgeAndQuestion();
        return assertMetric("PROMPT_INJECTION", "Prompt injection safety", threshold,
                () -> judge.evaluateContextPromptInjection(question, context));
    }

    /**
     * Asserts that the retrieved context exposes no personally identifiable information.
     *
     * @param threshold minimum acceptable compliance score in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the mean score is below the threshold or the judge is unstable
     */
    public ContextAssert hasNoPIILeak(double threshold) {
        requireJudgeAndQuestion();
        return assertMetric("PII_LEAK", "PII leak compliance", threshold,
                () -> judge.evaluateContextPIILeak(question, context));
    }

    private ContextAssert assertMetric(String type, String label, double threshold,
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
