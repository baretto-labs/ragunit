package org.ragunit.core.assertion;

import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.ReferenceAnswer;
import org.ragunit.core.domain.Verdict;
import org.ragunit.core.judge.RagJudge;
import org.ragunit.core.report.AssertionResult;
import org.ragunit.core.report.RagReporter;

import java.util.List;
import java.util.Objects;

/**
 * Builder for the Retrieval evaluation flow.
 *
 * <p>Obtain an instance via {@link RagAssert#assertThatContext(List)}.
 * Each method returns {@code this} for chaining. Calling {@link #hasRelevanceScore(double)}
 * triggers the Judge and throws {@link AssertionError} if the score is below the threshold.
 */
public final class ContextAssert {

    private final List<Document> context;
    private final List<RagReporter> reporters;
    private Question question;
    private RagJudge judge;

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
     * Asserts that the context relevance score meets the given threshold.
     * Notifies reporters whether the assertion passes or fails.
     *
     * @param threshold minimum acceptable relevance score in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the score is below the threshold
     */
    public ContextAssert hasRelevanceScore(double threshold) {
        Objects.requireNonNull(judge, "Call evaluatedBy() before asserting");
        Objects.requireNonNull(question, "Call forQuestion() before asserting");
        Verdict verdict = judge.evaluateRetrieval(question, context);
        boolean passed = verdict.isAboveThreshold(threshold);
        silentlyReport(new AssertionResult("CONTEXT_RELEVANCE", question, verdict, threshold, passed));
        if (!passed) {
            throw new AssertionError(buildMessage("Context relevance", verdict.score().value(), threshold));
        }
        return this;
    }

    /**
     * Asserts that the context was too insufficient to answer the question,
     * i.e. a refusal by the generator was justified.
     *
     * @param threshold minimum acceptable rejection-justification score in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the score is below the threshold
     */
    public ContextAssert correctlyRefusedToAnswer(double threshold) {
        Objects.requireNonNull(judge, "Call evaluatedBy() before asserting");
        Objects.requireNonNull(question, "Call forQuestion() before asserting");
        Verdict verdict = judge.evaluateContextRejection(question, context);
        boolean passed = verdict.isAboveThreshold(threshold);
        silentlyReport(new AssertionResult("REJECTION", question, verdict, threshold, passed));
        if (!passed) {
            throw new AssertionError(buildMessage("Rejection justification", verdict.score().value(), threshold));
        }
        return this;
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
     * @throws AssertionError if the score is below the threshold
     */
    public ContextAssert hasContextRecall(ReferenceAnswer reference, double threshold) {
        Objects.requireNonNull(judge, "Call evaluatedBy() before asserting");
        Objects.requireNonNull(question, "Call forQuestion() before asserting");
        Objects.requireNonNull(reference, "reference");
        Verdict verdict = judge.evaluateContextRecall(question, context, reference);
        boolean passed = verdict.isAboveThreshold(threshold);
        silentlyReport(new AssertionResult("CONTEXT_RECALL", question, verdict, threshold, passed));
        if (!passed) {
            throw new AssertionError(buildMessage("Context recall", verdict.score().value(), threshold));
        }
        return this;
    }

    /**
     * Asserts that the context precision score (RAGAS Average Precision) meets the threshold.
     * Evaluates each retrieved chunk independently and rewards top-ranked relevant chunks.
     *
     * @param threshold minimum acceptable precision score in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the score is below the threshold
     */
    public ContextAssert hasContextPrecision(double threshold) {
        Objects.requireNonNull(judge, "Call evaluatedBy() before asserting");
        Objects.requireNonNull(question, "Call forQuestion() before asserting");
        Verdict verdict = judge.evaluateContextPrecision(question, context);
        boolean passed = verdict.isAboveThreshold(threshold);
        silentlyReport(new AssertionResult("CONTEXT_PRECISION", question, verdict, threshold, passed));
        if (!passed) {
            throw new AssertionError(buildMessage("Context precision", verdict.score().value(), threshold));
        }
        return this;
    }

    /**
     * Asserts that the retrieved context contains no prompt injection attempts.
     *
     * @param threshold minimum acceptable safety score in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the score is below the threshold
     */
    public ContextAssert isSafeFromPromptInjection(double threshold) {
        Objects.requireNonNull(judge, "Call evaluatedBy() before asserting");
        Objects.requireNonNull(question, "Call forQuestion() before asserting");
        Verdict verdict = judge.evaluateContextPromptInjection(question, context);
        boolean passed = verdict.isAboveThreshold(threshold);
        silentlyReport(new AssertionResult("PROMPT_INJECTION", question, verdict, threshold, passed));
        if (!passed) {
            throw new AssertionError(buildMessage("Prompt injection safety", verdict.score().value(), threshold));
        }
        return this;
    }

    /**
     * Asserts that the retrieved context exposes no personally identifiable information.
     *
     * @param threshold minimum acceptable compliance score in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the score is below the threshold
     */
    public ContextAssert hasNoPIILeak(double threshold) {
        Objects.requireNonNull(judge, "Call evaluatedBy() before asserting");
        Objects.requireNonNull(question, "Call forQuestion() before asserting");
        Verdict verdict = judge.evaluateContextPIILeak(question, context);
        boolean passed = verdict.isAboveThreshold(threshold);
        silentlyReport(new AssertionResult("PII_LEAK", question, verdict, threshold, passed));
        if (!passed) {
            throw new AssertionError(buildMessage("PII leak compliance", verdict.score().value(), threshold));
        }
        return this;
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

    private static String buildMessage(String metric, double actual, double threshold) {
        return metric + " score " + actual + " is below threshold " + threshold;
    }
}
