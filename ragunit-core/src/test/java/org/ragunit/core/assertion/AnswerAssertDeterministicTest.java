package org.ragunit.core.assertion;

import org.junit.jupiter.api.Test;
import org.ragunit.core.domain.Answer;
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
 * Tests for the deterministic assertion helpers: hard checks (contains, regex,
 * length) that run without any LLM call and chain with the judged assertions.
 */
class AnswerAssertDeterministicTest {

    private static final Answer ANSWER =
            new Answer("To get a token, POST to /auth/token with your client_id.");
    private static final Question QUESTION = new Question("How do I get a token?");
    private static final List<Document> CONTEXT =
            List.of(new Document("POST /auth/token returns an access token."));
    private static final Verdict PASSING =
            Verdict.of(new Score(0.9), "ok", "stub");

    private final List<AssertionResult> captured = new ArrayList<>();
    private final List<RagReporter> reporters = List.of(captured::add);

    private AnswerAssert answerAssert() {
        return new AnswerAssert(ANSWER, reporters);
    }

    // --- contains ---

    @Test
    void should_pass_when_answerContainsExpectedText() {
        answerAssert().contains("/auth/token");
    }

    @Test
    void should_fail_when_answerDoesNotContainExpectedText() {
        assertThatThrownBy(() -> answerAssert().contains("/auth/refresh"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("/auth/refresh");
    }

    // --- containsAll ---

    @Test
    void should_pass_when_answerContainsAllExpectedTexts() {
        AnswerAssert chained = answerAssert().containsAll("POST", "client_id", "token");

        assertThat(chained).isNotNull();
    }

    @Test
    void should_failListingMissingTexts_when_someExpectedTextsAreAbsent() {
        assertThatThrownBy(() -> answerAssert().containsAll("POST", "refresh_token", "expires_in"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("refresh_token")
                .hasMessageContaining("expires_in")
                .hasMessageNotContaining("POST,");
    }

    // --- matches ---

    @Test
    void should_pass_when_answerMatchesRegex() {
        answerAssert().matches(".*POST to /auth/\\w+.*");
    }

    @Test
    void should_fail_when_answerDoesNotMatchRegex() {
        assertThatThrownBy(() -> answerAssert().matches("^GET .*"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("GET");
    }

    // --- hasMinLength ---

    @Test
    void should_pass_when_answerIsLongerThanMinLength() {
        answerAssert().hasMinLength(10);
    }

    @Test
    void should_pass_when_answerLengthEqualsMinLength() {
        answerAssert().hasMinLength(ANSWER.text().length());
    }

    @Test
    void should_fail_when_answerIsShorterThanMinLength() {
        assertThatThrownBy(() -> answerAssert().hasMinLength(10_000))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("10000");
    }

    // --- chaining with judged assertions ---

    @Test
    void should_chainDeterministicThenJudged_when_allPass() {
        answerAssert()
                .contains("/auth/token")
                .hasMinLength(10)
                .givenContext(CONTEXT)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubScriptedRagJudge(List.of(PASSING)))
                .isFaithfulToContext(0.8);
    }

    @Test
    void should_chainJudgedThenDeterministic_when_allPass() {
        answerAssert()
                .givenContext(CONTEXT)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubScriptedRagJudge(List.of(PASSING)))
                .isFaithfulToContext(0.8)
                .contains("client_id");
    }

    @Test
    void should_notCallJudge_when_runningDeterministicChecksOnly() {
        StubScriptedRagJudge judge = new StubScriptedRagJudge(List.of(PASSING));

        answerAssert()
                .evaluatedBy(judge)
                .contains("token")
                .matches(".*POST.*")
                .hasMinLength(5);

        assertThat(judge.callCount()).isZero();
    }

    @Test
    void should_notNotifyReporters_when_runningDeterministicChecks() {
        answerAssert().contains("token").hasMinLength(5);

        assertThat(captured).isEmpty();
    }

    // --- String overload on RagAssert ---

    @Test
    void should_startAssertionFromPlainText_when_usingStringOverload() {
        RagAssert.assertThatAnswer("plain text answer").contains("plain");
    }

    // --- null guards ---

    @Test
    void should_throwNullPointerException_when_containsArgumentIsNull() {
        assertThatThrownBy(() -> answerAssert().contains(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throwNullPointerException_when_matchesArgumentIsNull() {
        assertThatThrownBy(() -> answerAssert().matches(null))
                .isInstanceOf(NullPointerException.class);
    }
}
