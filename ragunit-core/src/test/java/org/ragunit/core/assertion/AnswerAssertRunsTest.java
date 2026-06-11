package org.ragunit.core.assertion;

import org.junit.jupiter.api.Test;
import org.ragunit.core.domain.Answer;
import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.ReferenceAnswer;
import org.ragunit.core.domain.Score;
import org.ragunit.core.domain.Verdict;
import org.ragunit.core.report.AssertionResult;
import org.ragunit.core.report.RagReporter;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for variance control on the Generation flow: {@code withRuns(n)} averages
 * the judge over n calls and fails when the judge is too unstable.
 */
class AnswerAssertRunsTest {

    private static final String MODEL = "stub-model";
    private static final Question QUESTION = new Question("What is the capital of France?");
    private static final Answer ANSWER = new Answer("Paris.");
    private static final List<Document> CONTEXT = List.of(new Document("Paris is the capital."));

    private static final Verdict STABLE_HIGH = Verdict.of(new Score(0.9), "high", MODEL);
    private static final Verdict STABLE_LOW = Verdict.of(new Score(0.5), "low", MODEL);

    private final List<AssertionResult> captured = new ArrayList<>();
    private final List<RagReporter> reporters = List.of(captured::add);

    private AnswerAssert answerAssert(StubScriptedRagJudge judge) {
        return new AnswerAssert(ANSWER, reporters)
                .givenContext(CONTEXT)
                .forQuestion(QUESTION)
                .evaluatedBy(judge);
    }

    @Test
    void should_callJudgeOncePerRun_when_withRunsConfigured() {
        StubScriptedRagJudge judge = new StubScriptedRagJudge(List.of(STABLE_HIGH));

        answerAssert(judge).withRuns(5).isFaithfulToContext(0.8);

        assertThat(judge.callCount()).isEqualTo(5);
    }

    @Test
    void should_callJudgeOnce_when_runsNotConfigured() {
        StubScriptedRagJudge judge = new StubScriptedRagJudge(List.of(STABLE_HIGH));

        answerAssert(judge).isFaithfulToContext(0.8);

        assertThat(judge.callCount()).isEqualTo(1);
    }

    @Test
    void should_failWithUnstableMessage_when_judgeAlternatesHighAndLowScores() {
        StubScriptedRagJudge judge = new StubScriptedRagJudge(List.of(STABLE_HIGH, STABLE_LOW));

        assertThatThrownBy(() -> answerAssert(judge).withRuns(4).isFaithfulToContext(0.6))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("judge too unstable")
                .hasMessageContaining("stddev");
    }

    @Test
    void should_failWithMeanMessage_when_meanBelowThresholdAndJudgeStable() {
        StubScriptedRagJudge judge = new StubScriptedRagJudge(List.of(STABLE_LOW));

        assertThatThrownBy(() -> answerAssert(judge).withRuns(3).isFaithfulToContext(0.8))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("mean")
                .hasMessageNotContaining("judge too unstable");
    }

    @Test
    void should_pass_when_meanAboveThresholdAndStddevWithinBound() {
        StubScriptedRagJudge judge = new StubScriptedRagJudge(
                List.of(Verdict.of(new Score(0.9), "r", MODEL), Verdict.of(new Score(0.85), "r", MODEL)));

        answerAssert(judge).withRuns(4).isFaithfulToContext(0.8);

        assertThat(captured).singleElement()
                .satisfies(result -> assertThat(result.passed()).isTrue());
    }

    @Test
    void should_reportMeanScore_when_runningMultipleTimes() {
        StubScriptedRagJudge judge = new StubScriptedRagJudge(
                List.of(Verdict.of(new Score(0.9), "r", MODEL), Verdict.of(new Score(0.8), "r", MODEL)));

        answerAssert(judge).withRuns(2).isFaithfulToContext(0.5);

        assertThat(captured.get(0).verdict().score().value())
                .isEqualTo(0.85, org.assertj.core.api.Assertions.within(1e-9));
    }

    @Test
    void should_failWhenStddevExceedsCustomBound_when_withMaxStddevConfigured() {
        StubScriptedRagJudge judge = new StubScriptedRagJudge(
                List.of(Verdict.of(new Score(0.9), "r", MODEL), Verdict.of(new Score(0.8), "r", MODEL)));

        assertThatThrownBy(() -> answerAssert(judge)
                .withRuns(2).withMaxStddev(0.01).isFaithfulToContext(0.5))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("judge too unstable");
    }

    @Test
    void should_applyRuns_when_assertingFactualCorrectness() {
        StubScriptedRagJudge judge = new StubScriptedRagJudge(List.of(STABLE_HIGH));

        answerAssert(judge)
                .comparedTo(new ReferenceAnswer("Paris."))
                .withRuns(3)
                .hasFactualCorrectnessF1(0.8);

        assertThat(judge.callCount()).isEqualTo(3);
    }

    @Test
    void should_reportOriginalVerdict_when_singleRun() {
        StubScriptedRagJudge judge = new StubScriptedRagJudge(List.of(STABLE_HIGH));

        answerAssert(judge).isFaithfulToContext(0.8);

        assertThat(captured.get(0).verdict()).isSameAs(STABLE_HIGH);
    }

    @Test
    void should_acceptSingleRun_when_withRunsIsOne() {
        StubScriptedRagJudge judge = new StubScriptedRagJudge(List.of(STABLE_HIGH));

        answerAssert(judge).withRuns(1).isFaithfulToContext(0.8);

        assertThat(judge.callCount()).isEqualTo(1);
    }

    @Test
    void should_acceptZeroBound_when_withMaxStddevIsZero() {
        StubScriptedRagJudge judge = new StubScriptedRagJudge(List.of(STABLE_HIGH));

        answerAssert(judge).withRuns(2).withMaxStddev(0.0).isFaithfulToContext(0.8);

        assertThat(judge.callCount()).isEqualTo(2);
    }

    @Test
    void should_pass_when_stddevEqualsMaxStddevExactly() {
        // scores 0.9 / 0.5 -> stddev exactly 0.2; bound 0.2 is inclusive
        StubScriptedRagJudge judge = new StubScriptedRagJudge(List.of(STABLE_HIGH, STABLE_LOW));

        answerAssert(judge).withRuns(2).withMaxStddev(0.2).isFaithfulToContext(0.6);

        assertThat(captured.get(0).passed()).isTrue();
    }

    @Test
    void should_returnSelf_when_factualRecallPasses() {
        StubScriptedRagJudge judge = new StubScriptedRagJudge(List.of(STABLE_HIGH));

        AnswerAssert chained = answerAssert(judge)
                .comparedTo(new ReferenceAnswer("Paris."))
                .hasFactualCorrectnessRecall(0.8);

        assertThat(chained).isNotNull();
    }

    @Test
    void should_throwNullPointerException_when_judgeIsMissingForAnyAssertion() {
        List<java.util.function.Consumer<AnswerAssert>> assertions = List.of(
                a -> a.isFaithfulToContext(0.5),
                a -> a.isRelevantToQuestion(0.5),
                a -> a.correctlyRefusedToAnswer(0.5),
                a -> a.hasValidToolTrajectory(List.of(), 0.5),
                a -> a.isSafeFromPromptInjection(0.5),
                a -> a.hasNoPIILeak(0.5),
                a -> a.hasFactualCorrectnessF1(0.5),
                a -> a.hasFactualCorrectnessPrecision(0.5),
                a -> a.hasFactualCorrectnessRecall(0.5));

        for (var assertion : assertions) {
            assertThatThrownBy(() -> assertion.accept(withoutJudge()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("evaluatedBy");
        }
    }

    private AnswerAssert withoutJudge() {
        return new AnswerAssert(ANSWER, reporters)
                .givenContext(CONTEXT)
                .forQuestion(QUESTION)
                .comparedTo(new ReferenceAnswer("Paris."));
    }

    @Test
    void should_throwIllegalArgumentException_when_runsIsZero() {
        StubScriptedRagJudge judge = new StubScriptedRagJudge(List.of(STABLE_HIGH));

        assertThatThrownBy(() -> answerAssert(judge).withRuns(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_throwIllegalArgumentException_when_maxStddevIsNegative() {
        StubScriptedRagJudge judge = new StubScriptedRagJudge(List.of(STABLE_HIGH));

        assertThatThrownBy(() -> answerAssert(judge).withMaxStddev(-0.1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
