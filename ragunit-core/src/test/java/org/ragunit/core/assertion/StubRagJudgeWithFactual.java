package org.ragunit.core.assertion;

import org.ragunit.core.domain.Answer;
import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.FactualCorrectnessVerdict;
import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.ReferenceAnswer;
import org.ragunit.core.domain.Score;
import org.ragunit.core.domain.ToolCall;
import org.ragunit.core.domain.Verdict;
import org.ragunit.core.judge.RagJudge;

import java.util.List;

/**
 * Test double for RagJudge that supports a configurable {@link FactualCorrectnessVerdict}.
 *
 * <p>All other evaluation methods return a default passing verdict.
 */
final class StubRagJudgeWithFactual implements RagJudge {

    private static final Verdict DEFAULT = Verdict.of(new Score(1.0), "stub", "stub");
    private final FactualCorrectnessVerdict factualVerdict;

    StubRagJudgeWithFactual(FactualCorrectnessVerdict factualVerdict) {
        this.factualVerdict = factualVerdict;
    }

    @Override
    public FactualCorrectnessVerdict evaluateFactualCorrectness(
            Question question, Answer answer, ReferenceAnswer reference) {
        return factualVerdict;
    }

    @Override
    public Verdict evaluateRetrieval(Question q, List<Document> c) {
        return DEFAULT;
    }

    @Override
    public Verdict evaluateGeneration(Question q, List<Document> c, Answer a) {
        return DEFAULT;
    }

    @Override
    public Verdict evaluateAnswerRelevancy(Question q, Answer a) {
        return DEFAULT;
    }

    @Override
    public Verdict evaluateContextRejection(Question q, List<Document> c) {
        return DEFAULT;
    }

    @Override
    public Verdict evaluateRejection(Question q, List<Document> c, Answer a) {
        return DEFAULT;
    }

    @Override
    public Verdict evaluateContextPromptInjection(Question q, List<Document> c) {
        return DEFAULT;
    }

    @Override
    public Verdict evaluatePromptInjection(Question q, List<Document> c, Answer a) {
        return DEFAULT;
    }

    @Override
    public Verdict evaluateContextPIILeak(Question q, List<Document> c) {
        return DEFAULT;
    }

    @Override
    public Verdict evaluatePIILeak(Question q, List<Document> c, Answer a) {
        return DEFAULT;
    }

    @Override
    public Verdict evaluateToolTrajectory(Question q, List<ToolCall> t, Answer a) {
        return DEFAULT;
    }

    @Override
    public Verdict evaluateContextPrecision(Question q, List<Document> c) {
        return DEFAULT;
    }

    @Override
    public Verdict evaluateContextRecall(Question q, List<Document> c, ReferenceAnswer r) {
        return DEFAULT;
    }
}
