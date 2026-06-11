package org.ragunit.core.assertion;

import org.junit.jupiter.api.Test;
import org.ragunit.core.domain.Answer;
import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.Score;
import org.ragunit.core.domain.ToolCall;
import org.ragunit.core.domain.Verdict;
import org.ragunit.core.report.AssertionResult;
import org.ragunit.core.report.RagReporter;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests for the Generation evaluation flow: RagAssert.assertThatAnswer(...). */
class AnswerAssertTest {

    private static final double HIGH = 0.95;
    private static final double LOW = 0.20;
    private static final double THRESHOLD = 0.85;
    private static final String MODEL = "stub-model";
    private static final String RATIONALE = "stub rationale";

    private static final Verdict PASSING_VERDICT = Verdict.of(new Score(HIGH), RATIONALE, MODEL);
    private static final Verdict FAILING_VERDICT = Verdict.of(new Score(LOW), RATIONALE, MODEL);
    private static final Question QUESTION = new Question("What is the capital of France?");
    private static final Answer ANSWER = new Answer("Paris is the capital of France.");
    private static final Document DOC = new Document("Paris is the capital of France.");
    private static final List<Document> CONTEXT = List.of(DOC);

    private final List<AssertionResult> captured = new ArrayList<>();
    private final List<RagReporter> reporters = List.of(captured::add);

    @Test
    void should_pass_when_answerIsFaithfulAboveThreshold() {
        new AnswerAssert(ANSWER, reporters)
                .givenContext(CONTEXT)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT))
                .isFaithfulToContext(THRESHOLD);
    }

    @Test
    void should_fail_when_answerIsNotFaithful() {
        assertThatThrownBy(() ->
                new AnswerAssert(ANSWER, reporters)
                        .givenContext(CONTEXT)
                        .forQuestion(QUESTION)
                        .evaluatedBy(new StubRagJudge(PASSING_VERDICT, FAILING_VERDICT))
                        .isFaithfulToContext(THRESHOLD)
        ).isInstanceOf(AssertionError.class);
    }

    @Test
    void should_pass_when_answerIsRelevantAboveThreshold() {
        new AnswerAssert(ANSWER, reporters)
                .givenContext(CONTEXT)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT))
                .isRelevantToQuestion(THRESHOLD);
    }

    @Test
    void should_chainMultipleAssertions_when_allPass() {
        new AnswerAssert(ANSWER, reporters)
                .givenContext(CONTEXT)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT))
                .isFaithfulToContext(THRESHOLD)
                .isRelevantToQuestion(THRESHOLD);
    }

    @Test
    void should_failFast_when_firstAssertionFails() {
        assertThatThrownBy(() ->
                new AnswerAssert(ANSWER, reporters)
                        .givenContext(CONTEXT)
                        .forQuestion(QUESTION)
                        .evaluatedBy(new StubRagJudge(PASSING_VERDICT, FAILING_VERDICT))
                        .isFaithfulToContext(THRESHOLD)
                        .isRelevantToQuestion(THRESHOLD)
        ).isInstanceOf(AssertionError.class);
    }

    @Test
    void should_includeScoreInErrorMessage_when_answerIsNotRelevant() {
        assertThatThrownBy(() ->
                new AnswerAssert(ANSWER, reporters)
                        .givenContext(CONTEXT)
                        .forQuestion(QUESTION)
                        .evaluatedBy(new StubRagJudge(PASSING_VERDICT, FAILING_VERDICT))
                        .isRelevantToQuestion(THRESHOLD)
        ).isInstanceOf(AssertionError.class)
                .hasMessageContaining(String.valueOf(LOW));
    }

    @Test
    void should_notifyReporterTwice_when_twoAssertionsComplete() {
        new AnswerAssert(ANSWER, reporters)
                .givenContext(CONTEXT)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT, PASSING_VERDICT))
                .isFaithfulToContext(THRESHOLD)
                .isRelevantToQuestion(THRESHOLD);

        assertThat(captured).hasSize(2);
    }

    // --- correctlyRefusedToAnswer ---

    @Test
    void should_pass_when_answerIndicatesCorrectRefusal() {
        new AnswerAssert(ANSWER, reporters)
                .givenContext(CONTEXT)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT, PASSING_VERDICT))
                .correctlyRefusedToAnswer(THRESHOLD);
    }

    @Test
    void should_fail_when_answerAnswersWhileContextIsInsufficient() {
        assertThatThrownBy(() ->
                new AnswerAssert(ANSWER, reporters)
                        .givenContext(CONTEXT)
                        .forQuestion(QUESTION)
                        .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT, FAILING_VERDICT))
                        .correctlyRefusedToAnswer(THRESHOLD)
        ).isInstanceOf(AssertionError.class);
    }

    @Test
    void should_reportRejectionAssertion_when_correctlyRefusedToAnswerPasses() {
        new AnswerAssert(ANSWER, reporters)
                .givenContext(CONTEXT)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT, PASSING_VERDICT))
                .correctlyRefusedToAnswer(THRESHOLD);

        assertThat(captured).hasSize(1);
        assertThat(captured.get(0).assertionType()).isEqualTo("REJECTION");
        assertThat(captured.get(0).passed()).isTrue();
    }

    // --- isSafeFromPromptInjection ---

    @Test
    void should_pass_when_answerContainsNoInjectionAttempt() {
        new AnswerAssert(ANSWER, reporters)
                .givenContext(CONTEXT)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT, PASSING_VERDICT))
                .isSafeFromPromptInjection(THRESHOLD);
    }

    @Test
    void should_fail_when_answerRepeatsInjectedInstruction() {
        assertThatThrownBy(() ->
                new AnswerAssert(ANSWER, reporters)
                        .givenContext(CONTEXT)
                        .forQuestion(QUESTION)
                        .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT, FAILING_VERDICT))
                        .isSafeFromPromptInjection(THRESHOLD)
        ).isInstanceOf(AssertionError.class)
                .hasMessageContaining(String.valueOf(LOW));
    }

    @Test
    void should_reportPromptInjectionAssertion_when_answerIsSafe() {
        new AnswerAssert(ANSWER, reporters)
                .givenContext(CONTEXT)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT, PASSING_VERDICT))
                .isSafeFromPromptInjection(THRESHOLD);

        assertThat(captured).hasSize(1);
        assertThat(captured.get(0).assertionType()).isEqualTo("PROMPT_INJECTION");
        assertThat(captured.get(0).passed()).isTrue();
    }

    // --- hasNoPIILeak ---

    @Test
    void should_pass_when_answerContainsNoPII() {
        new AnswerAssert(ANSWER, reporters)
                .givenContext(CONTEXT)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT, PASSING_VERDICT))
                .hasNoPIILeak(THRESHOLD);
    }

    @Test
    void should_fail_when_answerRevealsSensitivePersonalData() {
        assertThatThrownBy(() ->
                new AnswerAssert(ANSWER, reporters)
                        .givenContext(CONTEXT)
                        .forQuestion(QUESTION)
                        .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT, FAILING_VERDICT))
                        .hasNoPIILeak(THRESHOLD)
        ).isInstanceOf(AssertionError.class)
                .hasMessageContaining(String.valueOf(LOW));
    }

    @Test
    void should_reportPIILeakAssertion_when_answerIsClean() {
        new AnswerAssert(ANSWER, reporters)
                .givenContext(CONTEXT)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT, PASSING_VERDICT))
                .hasNoPIILeak(THRESHOLD);

        assertThat(captured).hasSize(1);
        assertThat(captured.get(0).assertionType()).isEqualTo("PII_LEAK");
        assertThat(captured.get(0).passed()).isTrue();
    }

    // --- isRelevantToQuestion (evaluateAnswerRelevancy — hypothetical questions) ---

    @Test
    void should_pass_when_answerDirectlyAddressesQuestion() {
        new AnswerAssert(ANSWER, reporters)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT))
                .isRelevantToQuestion(THRESHOLD);
    }

    @Test
    void should_fail_when_answerIsVagueAndOffTopic() {
        assertThatThrownBy(() ->
                new AnswerAssert(ANSWER, reporters)
                        .forQuestion(QUESTION)
                        .evaluatedBy(new StubRagJudge(PASSING_VERDICT, FAILING_VERDICT))
                        .isRelevantToQuestion(THRESHOLD)
        ).isInstanceOf(AssertionError.class)
                .hasMessageContaining(String.valueOf(LOW));
    }

    @Test
    void should_reportAnswerRelevancyAssertion_when_answerIsRelevant() {
        new AnswerAssert(ANSWER, reporters)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT))
                .isRelevantToQuestion(THRESHOLD);

        assertThat(captured).hasSize(1);
        assertThat(captured.get(0).assertionType()).isEqualTo("ANSWER_RELEVANCY");
        assertThat(captured.get(0).passed()).isTrue();
    }

    // --- hasValidToolTrajectory ---

    private static final ToolCall TOOL_CALL = new ToolCall("web_search", "capital of France", "Paris");
    private static final List<ToolCall> TRAJECTORY = List.of(TOOL_CALL);

    @Test
    void should_pass_when_trajectoryContainsAllRequiredToolCalls() {
        new AnswerAssert(ANSWER, reporters)
                .givenContext(CONTEXT)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT, PASSING_VERDICT))
                .hasValidToolTrajectory(TRAJECTORY, THRESHOLD);
    }

    @Test
    void should_fail_when_trajectoryIsMissingCriticalToolCall() {
        assertThatThrownBy(() ->
                new AnswerAssert(ANSWER, reporters)
                        .givenContext(CONTEXT)
                        .forQuestion(QUESTION)
                        .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT, FAILING_VERDICT))
                        .hasValidToolTrajectory(TRAJECTORY, THRESHOLD)
        ).isInstanceOf(AssertionError.class);
    }

    @Test
    void should_throwAssertionError_when_trajectoryScoreIsBelowThreshold() {
        assertThatThrownBy(() ->
                new AnswerAssert(ANSWER, reporters)
                        .givenContext(CONTEXT)
                        .forQuestion(QUESTION)
                        .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT, FAILING_VERDICT))
                        .hasValidToolTrajectory(TRAJECTORY, THRESHOLD)
        ).isInstanceOf(AssertionError.class)
                .hasMessageContaining(String.valueOf(LOW));
    }

    @Test
    void should_reportToolTrajectoryAssertion_when_trajectoryIsValid() {
        new AnswerAssert(ANSWER, reporters)
                .givenContext(CONTEXT)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT, PASSING_VERDICT))
                .hasValidToolTrajectory(TRAJECTORY, THRESHOLD);

        assertThat(captured).hasSize(1);
        assertThat(captured.get(0).assertionType()).isEqualTo("TOOL_TRAJECTORY");
        assertThat(captured.get(0).passed()).isTrue();
    }

    @Test
    void should_returnThis_when_chainingAllAnswerSafetyAssertions() {
        AnswerAssert result = new AnswerAssert(ANSWER, reporters)
                .givenContext(CONTEXT)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT, PASSING_VERDICT))
                .isRelevantToQuestion(THRESHOLD)
                .correctlyRefusedToAnswer(THRESHOLD)
                .isSafeFromPromptInjection(THRESHOLD)
                .hasNoPIILeak(THRESHOLD)
                .hasValidToolTrajectory(TRAJECTORY, THRESHOLD);
        assertThat(result).isNotNull();
    }
}
