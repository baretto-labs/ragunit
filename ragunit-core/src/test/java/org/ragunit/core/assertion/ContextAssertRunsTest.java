package org.ragunit.core.assertion;

import org.junit.jupiter.api.Test;
import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.Score;
import org.ragunit.core.domain.Verdict;
import org.ragunit.core.report.AssertionResult;
import org.ragunit.core.report.RagReporter;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for variance control on the Retrieval flow: {@code withRuns(n)} averages
 * the judge over n calls and fails when the judge is too unstable.
 */
class ContextAssertRunsTest {

    private static final String MODEL = "stub-model";
    private static final Question QUESTION = new Question("What is the capital of France?");
    private static final List<Document> CONTEXT = List.of(new Document("Paris is the capital."));

    private static final Verdict STABLE_HIGH = Verdict.of(new Score(0.9), "high", MODEL);
    private static final Verdict STABLE_LOW = Verdict.of(new Score(0.5), "low", MODEL);

    private final List<AssertionResult> captured = new ArrayList<>();
    private final List<RagReporter> reporters = List.of(captured::add);

    private ContextAssert contextAssert(StubScriptedRagJudge judge) {
        return new ContextAssert(CONTEXT, reporters)
                .forQuestion(QUESTION)
                .evaluatedBy(judge);
    }

    @Test
    void should_callJudgeOncePerRun_when_withRunsConfigured() {
        StubScriptedRagJudge judge = new StubScriptedRagJudge(List.of(STABLE_HIGH));

        contextAssert(judge).withRuns(3).hasRelevanceScore(0.8);

        assertThat(judge.callCount()).isEqualTo(3);
    }

    @Test
    void should_failWithUnstableMessage_when_judgeAlternatesHighAndLowScores() {
        StubScriptedRagJudge judge = new StubScriptedRagJudge(List.of(STABLE_HIGH, STABLE_LOW));

        assertThatThrownBy(() -> contextAssert(judge).withRuns(4).hasRelevanceScore(0.6))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("judge too unstable");
    }

    @Test
    void should_pass_when_meanAboveThresholdAndStddevWithinBound() {
        StubScriptedRagJudge judge = new StubScriptedRagJudge(List.of(STABLE_HIGH));

        contextAssert(judge).withRuns(2).hasRelevanceScore(0.8);

        assertThat(captured).singleElement()
                .satisfies(result -> assertThat(result.passed()).isTrue());
    }

    @Test
    void should_throwIllegalArgumentException_when_runsIsZero() {
        StubScriptedRagJudge judge = new StubScriptedRagJudge(List.of(STABLE_HIGH));

        assertThatThrownBy(() -> contextAssert(judge).withRuns(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_acceptSingleRun_when_withRunsIsOne() {
        StubScriptedRagJudge judge = new StubScriptedRagJudge(List.of(STABLE_HIGH));

        contextAssert(judge).withRuns(1).hasRelevanceScore(0.8);

        assertThat(judge.callCount()).isEqualTo(1);
    }

    @Test
    void should_throwIllegalArgumentException_when_maxStddevIsNegative() {
        StubScriptedRagJudge judge = new StubScriptedRagJudge(List.of(STABLE_HIGH));

        assertThatThrownBy(() -> contextAssert(judge).withMaxStddev(-0.1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_acceptZeroBound_when_withMaxStddevIsZero() {
        StubScriptedRagJudge judge = new StubScriptedRagJudge(List.of(STABLE_HIGH));

        contextAssert(judge).withRuns(2).withMaxStddev(0.0).hasRelevanceScore(0.8);

        assertThat(judge.callCount()).isEqualTo(2);
    }

    @Test
    void should_throwNullPointerException_when_judgeIsMissingForAnyAssertion() {
        List<java.util.function.Consumer<ContextAssert>> assertions = List.of(
                c -> c.hasRelevanceScore(0.5),
                c -> c.correctlyRefusedToAnswer(0.5),
                c -> c.hasContextRecall(new org.ragunit.core.domain.ReferenceAnswer("Paris."), 0.5),
                c -> c.hasContextPrecision(0.5),
                c -> c.isSafeFromPromptInjection(0.5),
                c -> c.hasNoPIILeak(0.5));

        for (var assertion : assertions) {
            ContextAssert withoutJudge = new ContextAssert(CONTEXT, reporters)
                    .forQuestion(QUESTION);
            assertThatThrownBy(() -> assertion.accept(withoutJudge))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("evaluatedBy");
        }
    }
}
