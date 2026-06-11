package org.ragunit.core.assertion;

import org.ragunit.core.domain.Answer;
import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.FactualCorrectnessVerdict;
import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.ReferenceAnswer;
import org.ragunit.core.domain.ToolCall;
import org.ragunit.core.domain.Verdict;
import org.ragunit.core.judge.RagJudge;
import org.ragunit.core.report.AssertionResult;
import org.ragunit.core.report.RagReporter;

import java.util.List;
import java.util.Objects;

/**
 * Builder for the Generation evaluation flow.
 *
 * <p>Obtain an instance via {@link RagAssert#assertThatAnswer(Answer)}.
 * Each assertion method calls the judge independently — one LLM call per metric.
 */
public final class AnswerAssert {

    private final Answer answer;
    private final List<RagReporter> reporters;
    private List<Document> context;
    private Question question;
    private RagJudge judge;
    private ReferenceAnswer reference;

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
     * Asserts that the answer is faithful to the given context.
     *
     * @param threshold minimum acceptable faithfulness score in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the score is below the threshold
     */
    public AnswerAssert isFaithfulToContext(double threshold) {
        Objects.requireNonNull(judge, "Call evaluatedBy() before asserting");
        Objects.requireNonNull(question, "Call forQuestion() before asserting");
        Objects.requireNonNull(context, "Call givenContext() before asserting");
        Verdict verdict = judge.evaluateGeneration(question, context, answer);
        boolean passed = verdict.isAboveThreshold(threshold);
        silentlyReport(new AssertionResult("FAITHFULNESS", question, verdict, threshold, passed));
        if (!passed) {
            throw new AssertionError(buildMessage("Faithfulness", verdict.score().value(), threshold));
        }
        return this;
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
     * @throws AssertionError if the score is below the threshold
     */
    public AnswerAssert isRelevantToQuestion(double threshold) {
        Objects.requireNonNull(judge, "Call evaluatedBy() before asserting");
        Objects.requireNonNull(question, "Call forQuestion() before asserting");
        Verdict verdict = judge.evaluateAnswerRelevancy(question, answer);
        boolean passed = verdict.isAboveThreshold(threshold);
        silentlyReport(new AssertionResult("ANSWER_RELEVANCY", question, verdict, threshold, passed));
        if (!passed) {
            throw new AssertionError(buildMessage("Answer relevancy", verdict.score().value(), threshold));
        }
        return this;
    }

    /**
     * Asserts that the answer correctly indicates a refusal AND that refusal was
     * justified given the context and question.
     *
     * @param threshold minimum acceptable rejection-justification score in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the score is below the threshold
     */
    public AnswerAssert correctlyRefusedToAnswer(double threshold) {
        Objects.requireNonNull(judge, "Call evaluatedBy() before asserting");
        Objects.requireNonNull(question, "Call forQuestion() before asserting");
        Objects.requireNonNull(context, "Call givenContext() before asserting");
        Verdict verdict = judge.evaluateRejection(question, context, answer);
        boolean passed = verdict.isAboveThreshold(threshold);
        silentlyReport(new AssertionResult("REJECTION", question, verdict, threshold, passed));
        if (!passed) {
            throw new AssertionError(buildMessage("Rejection justification", verdict.score().value(), threshold));
        }
        return this;
    }

    /**
     * Asserts that the sequence of tool calls is necessary, sufficient,
     * and in the correct order to answer the question.
     *
     * @param trajectory the ordered list of tool calls produced by the Generator
     * @param threshold  minimum acceptable trajectory quality score in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the score is below the threshold
     */
    public AnswerAssert hasValidToolTrajectory(List<ToolCall> trajectory, double threshold) {
        Objects.requireNonNull(judge, "Call evaluatedBy() before asserting");
        Objects.requireNonNull(question, "Call forQuestion() before asserting");
        Objects.requireNonNull(trajectory, "trajectory");
        Verdict verdict = judge.evaluateToolTrajectory(question, trajectory, answer);
        boolean passed = verdict.isAboveThreshold(threshold);
        silentlyReport(new AssertionResult("TOOL_TRAJECTORY", question, verdict, threshold, passed));
        if (!passed) {
            throw new AssertionError(buildMessage("Tool trajectory quality", verdict.score().value(), threshold));
        }
        return this;
    }

    /**
     * Asserts that the generated answer contains no prompt injection content.
     *
     * @param threshold minimum acceptable safety score in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the score is below the threshold
     */
    public AnswerAssert isSafeFromPromptInjection(double threshold) {
        Objects.requireNonNull(judge, "Call evaluatedBy() before asserting");
        Objects.requireNonNull(question, "Call forQuestion() before asserting");
        Objects.requireNonNull(context, "Call givenContext() before asserting");
        Verdict verdict = judge.evaluatePromptInjection(question, context, answer);
        boolean passed = verdict.isAboveThreshold(threshold);
        silentlyReport(new AssertionResult("PROMPT_INJECTION", question, verdict, threshold, passed));
        if (!passed) {
            throw new AssertionError(buildMessage("Prompt injection safety", verdict.score().value(), threshold));
        }
        return this;
    }

    /**
     * Asserts that the generated answer leaks no personally identifiable information.
     *
     * @param threshold minimum acceptable compliance score in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the score is below the threshold
     */
    public AnswerAssert hasNoPIILeak(double threshold) {
        Objects.requireNonNull(judge, "Call evaluatedBy() before asserting");
        Objects.requireNonNull(question, "Call forQuestion() before asserting");
        Objects.requireNonNull(context, "Call givenContext() before asserting");
        Verdict verdict = judge.evaluatePIILeak(question, context, answer);
        boolean passed = verdict.isAboveThreshold(threshold);
        silentlyReport(new AssertionResult("PII_LEAK", question, verdict, threshold, passed));
        if (!passed) {
            throw new AssertionError(buildMessage("PII leak compliance", verdict.score().value(), threshold));
        }
        return this;
    }

    /**
     * Asserts that the answer's F1 factual correctness score meets the threshold.
     *
     * <p>F1 = harmonic mean of precision (Answer claims in Reference) and
     * recall (Reference claims in Answer). Requires {@link #comparedTo(ReferenceAnswer)}.
     *
     * @param threshold minimum acceptable F1 score in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the F1 score is below the threshold
     */
    public AnswerAssert hasFactualCorrectnessF1(double threshold) {
        FactualCorrectnessVerdict verdict = evaluateFactualCorrectness();
        boolean passed = verdict.isF1AboveThreshold(threshold);
        reportFactual(verdict, threshold, passed);
        if (!passed) {
            throw new AssertionError(buildMessage("Factual correctness F1", verdict.f1().value(), threshold));
        }
        return this;
    }

    /**
     * Asserts that the answer's factual precision meets the threshold.
     *
     * <p>Precision = fraction of Answer claims supported by the ReferenceAnswer.
     *
     * @param threshold minimum acceptable precision in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the precision score is below the threshold
     */
    public AnswerAssert hasFactualCorrectnessPrecision(double threshold) {
        FactualCorrectnessVerdict verdict = evaluateFactualCorrectness();
        boolean passed = verdict.isPrecisionAboveThreshold(threshold);
        reportFactual(verdict, threshold, passed);
        if (!passed) {
            throw new AssertionError(
                    buildMessage("Factual correctness precision", verdict.precision().value(), threshold));
        }
        return this;
    }

    /**
     * Asserts that the answer's factual recall meets the threshold.
     *
     * <p>Recall = fraction of ReferenceAnswer claims covered by the Answer.
     *
     * @param threshold minimum acceptable recall in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the recall score is below the threshold
     */
    public AnswerAssert hasFactualCorrectnessRecall(double threshold) {
        FactualCorrectnessVerdict verdict = evaluateFactualCorrectness();
        boolean passed = verdict.isRecallAboveThreshold(threshold);
        reportFactual(verdict, threshold, passed);
        if (!passed) {
            throw new AssertionError(
                    buildMessage("Factual correctness recall", verdict.recall().value(), threshold));
        }
        return this;
    }

    private FactualCorrectnessVerdict evaluateFactualCorrectness() {
        Objects.requireNonNull(judge, "Call evaluatedBy() before asserting");
        Objects.requireNonNull(question, "Call forQuestion() before asserting");
        Objects.requireNonNull(reference, "Call comparedTo() before asserting factual correctness");
        return judge.evaluateFactualCorrectness(question, answer, reference);
    }

    private void reportFactual(FactualCorrectnessVerdict verdict, double threshold, boolean passed) {
        Verdict wrapped = Verdict.of(verdict.f1(), verdict.rationale(), verdict.model());
        silentlyReport(new AssertionResult("FACTUAL_CORRECTNESS", question, wrapped, threshold, passed));
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
