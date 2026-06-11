package org.ragunit.core.assertion;

import org.ragunit.core.domain.Answer;
import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.FactualCorrectnessVerdict;
import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.ReferenceAnswer;
import org.ragunit.core.domain.ToolCall;
import org.ragunit.core.domain.Verdict;
import org.ragunit.core.judge.RagJudge;

import java.util.List;

/**
 * Test double: returns pre-configured verdicts for all evaluation methods.
 *
 * <p>{@code safetyVerdict} is shared by all safety metrics (rejection, prompt injection,
 * PII leak) so individual tests can control pass/fail without enumerating every method.
 */
final class StubRagJudge implements RagJudge {

    private final Verdict retrievalVerdict;
    private final Verdict generationVerdict;
    private final Verdict safetyVerdict;

    StubRagJudge(Verdict retrievalVerdict, Verdict generationVerdict) {
        this(retrievalVerdict, generationVerdict, retrievalVerdict);
    }

    StubRagJudge(Verdict retrievalVerdict, Verdict generationVerdict, Verdict safetyVerdict) {
        this.retrievalVerdict = retrievalVerdict;
        this.generationVerdict = generationVerdict;
        this.safetyVerdict = safetyVerdict;
    }

    @Override
    public FactualCorrectnessVerdict evaluateFactualCorrectness(
            Question question, Answer answer, ReferenceAnswer reference) {
        return new FactualCorrectnessVerdict(
                retrievalVerdict.score(), retrievalVerdict.score(), retrievalVerdict.score(),
                retrievalVerdict.rationale(), retrievalVerdict.model());
    }

    @Override
    public Verdict evaluateRetrieval(Question question, List<Document> context) {
        return retrievalVerdict;
    }

    @Override
    public Verdict evaluateGeneration(Question question, List<Document> context, Answer answer) {
        return generationVerdict;
    }

    @Override
    public Verdict evaluateAnswerRelevancy(Question question, Answer answer) {
        return generationVerdict;
    }

    @Override
    public Verdict evaluateContextRejection(Question question, List<Document> context) {
        return safetyVerdict;
    }

    @Override
    public Verdict evaluateRejection(Question question, List<Document> context, Answer answer) {
        return safetyVerdict;
    }

    @Override
    public Verdict evaluateContextPromptInjection(Question question, List<Document> context) {
        return safetyVerdict;
    }

    @Override
    public Verdict evaluatePromptInjection(Question question, List<Document> context, Answer answer) {
        return safetyVerdict;
    }

    @Override
    public Verdict evaluateContextPIILeak(Question question, List<Document> context) {
        return safetyVerdict;
    }

    @Override
    public Verdict evaluatePIILeak(Question question, List<Document> context, Answer answer) {
        return safetyVerdict;
    }

    @Override
    public Verdict evaluateToolTrajectory(Question question, List<ToolCall> trajectory, Answer answer) {
        return safetyVerdict;
    }

    @Override
    public Verdict evaluateContextPrecision(Question question, List<Document> context) {
        return retrievalVerdict;
    }

    @Override
    public Verdict evaluateContextRecall(Question question, List<Document> context, ReferenceAnswer reference) {
        return retrievalVerdict;
    }
}
