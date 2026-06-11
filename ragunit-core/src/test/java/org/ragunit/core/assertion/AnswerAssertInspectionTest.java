package org.ragunit.core.assertion;

import org.junit.jupiter.api.Test;
import org.ragunit.core.domain.Answer;
import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.Score;
import org.ragunit.core.domain.Verdict;
import org.ragunit.core.judge.JudgeResult;
import org.ragunit.core.report.RagReporter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Tests for judge-result inspection: failure messages carry the judge's
 * justification, and the full {@link JudgeResult} stays retrievable.
 */
class AnswerAssertInspectionTest {

    private static final String MODEL = "stub-model";
    private static final Question QUESTION = new Question("What is the capital of France?");
    private static final Answer ANSWER = new Answer("Paris.");
    private static final List<Document> CONTEXT = List.of(new Document("Paris is the capital."));
    private static final List<RagReporter> NO_REPORTERS = List.of();

    private static AnswerAssert answerAssert(Verdict verdict) {
        return new AnswerAssert(ANSWER, NO_REPORTERS)
                .givenContext(CONTEXT)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubScriptedRagJudge(List.of(verdict)));
    }

    @Test
    void should_includeJustificationInFailureMessage_when_assertionFails() {
        Verdict failing = Verdict.of(new Score(0.2), "Two claims contradict the context.", MODEL);

        assertThatThrownBy(() -> answerAssert(failing).isFaithfulToContext(0.8))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Two claims contradict the context.");
    }

    @Test
    void should_truncateJustificationInFailureMessage_when_rationaleExceeds500Chars() {
        String longRationale = "x".repeat(600);
        Verdict failing = Verdict.of(new Score(0.2), longRationale, MODEL);

        Throwable error = catchThrowable(() -> answerAssert(failing).isFaithfulToContext(0.8));

        assertThat(error).isInstanceOf(AssertionError.class);
        assertThat(error.getMessage()).contains("x".repeat(500) + "…");
        assertThat(error.getMessage()).doesNotContain("x".repeat(501));
    }

    @Test
    void should_notTruncateJustification_when_rationaleIsExactly500Chars() {
        String exactRationale = "y".repeat(500);
        Verdict failing = Verdict.of(new Score(0.2), exactRationale, MODEL);

        Throwable error = catchThrowable(() -> answerAssert(failing).isFaithfulToContext(0.8));

        assertThat(error.getMessage()).contains(exactRationale);
        assertThat(error.getMessage()).doesNotContain("…");
    }

    @Test
    void should_exposeJudgeResult_when_assertionPasses() {
        Verdict passing = Verdict.of(new Score(0.9), "All claims supported.", MODEL)
                .withExchange("THE PROMPT", "{\"score\": 0.9}");
        AnswerAssert assertion = answerAssert(passing);

        assertion.isFaithfulToContext(0.8);

        assertThat(assertion.lastJudgeResult()).hasValueSatisfying(result -> {
            assertThat(result.score()).isEqualTo(0.9);
            assertThat(result.justification()).isEqualTo("All claims supported.");
            assertThat(result.promptUsed()).isEqualTo("THE PROMPT");
            assertThat(result.rawResponse()).isEqualTo("{\"score\": 0.9}");
        });
    }

    @Test
    void should_exposeJudgeResult_when_assertionFails() {
        Verdict failing = Verdict.of(new Score(0.2), "Unsupported claims.", MODEL);
        AnswerAssert assertion = answerAssert(failing);

        catchThrowable(() -> assertion.isFaithfulToContext(0.8));

        assertThat(assertion.lastJudgeResult()).hasValueSatisfying(result ->
                assertThat(result.justification()).isEqualTo("Unsupported claims."));
    }

    @Test
    void should_returnEmptyJudgeResult_when_noAssertionRanYet() {
        Verdict any = Verdict.of(new Score(0.9), "r", MODEL);

        assertThat(answerAssert(any).lastJudgeResult()).isEmpty();
    }
}
