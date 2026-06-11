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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test double: cycles through a scripted list of verdicts on every evaluation call,
 * regardless of the metric, and counts how many times it was called.
 *
 * <p>Models an unstable judge (e.g. alternating 0.9 / 0.5) for variance-control tests.
 */
final class StubScriptedRagJudge implements RagJudge {

    private final List<Verdict> script;
    private final AtomicInteger calls = new AtomicInteger();

    StubScriptedRagJudge(List<Verdict> script) {
        this.script = List.copyOf(script);
    }

    int callCount() {
        return calls.get();
    }

    private Verdict next() {
        return script.get(calls.getAndIncrement() % script.size());
    }

    @Override
    public Verdict evaluateRetrieval(Question question, List<Document> context) {
        return next();
    }

    @Override
    public Verdict evaluateGeneration(Question question, List<Document> context, Answer answer) {
        return next();
    }

    @Override
    public Verdict evaluateContextRejection(Question question, List<Document> context) {
        return next();
    }

    @Override
    public Verdict evaluateRejection(Question question, List<Document> context, Answer answer) {
        return next();
    }

    @Override
    public Verdict evaluateContextPromptInjection(Question question, List<Document> context) {
        return next();
    }

    @Override
    public Verdict evaluatePromptInjection(Question question, List<Document> context, Answer answer) {
        return next();
    }

    @Override
    public Verdict evaluateContextPIILeak(Question question, List<Document> context) {
        return next();
    }

    @Override
    public Verdict evaluatePIILeak(Question question, List<Document> context, Answer answer) {
        return next();
    }

    @Override
    public Verdict evaluateAnswerRelevancy(Question question, Answer answer) {
        return next();
    }

    @Override
    public Verdict evaluateContextPrecision(Question question, List<Document> context) {
        return next();
    }

    @Override
    public Verdict evaluateContextRecall(Question question, List<Document> context, ReferenceAnswer reference) {
        return next();
    }

    @Override
    public FactualCorrectnessVerdict evaluateFactualCorrectness(
            Question question, Answer answer, ReferenceAnswer reference) {
        Verdict verdict = next();
        return new FactualCorrectnessVerdict(verdict.score(), verdict.score(), verdict.score(),
                verdict.rationale(), verdict.model());
    }

    @Override
    public Verdict evaluateToolTrajectory(Question question, List<ToolCall> trajectory, Answer answer) {
        return next();
    }
}
