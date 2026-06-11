package org.ragunit.core.assertion;

import org.junit.jupiter.api.Test;
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

/** Tests for the Retrieval evaluation flow: RagAssert.assertThatContext(...). */
class ContextAssertTest {

    private static final double HIGH = 0.95;
    private static final double LOW = 0.20;
    private static final double THRESHOLD = 0.85;
    private static final String MODEL = "stub-model";
    private static final String RATIONALE = "stub rationale";

    private static final Verdict PASSING_VERDICT = Verdict.of(new Score(HIGH), RATIONALE, MODEL);
    private static final Verdict FAILING_VERDICT = Verdict.of(new Score(LOW), RATIONALE, MODEL);
    private static final Question QUESTION = new Question("What is the capital of France?");
    private static final Document DOC = new Document("Paris is the capital of France.");
    private static final List<Document> CONTEXT = List.of(DOC);

    private final List<AssertionResult> captured = new ArrayList<>();
    private final List<RagReporter> reporters = List.of(captured::add);

    @Test
    void should_pass_when_contextRelevanceScoreIsAboveThreshold() {
        new ContextAssert(CONTEXT, reporters)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT))
                .hasRelevanceScore(THRESHOLD);
    }

    @Test
    void should_fail_when_contextRelevanceScoreIsBelowThreshold() {
        assertThatThrownBy(() ->
                new ContextAssert(CONTEXT, reporters)
                        .forQuestion(QUESTION)
                        .evaluatedBy(new StubRagJudge(FAILING_VERDICT, FAILING_VERDICT))
                        .hasRelevanceScore(THRESHOLD)
        ).isInstanceOf(AssertionError.class);
    }

    @Test
    void should_includeScoreInErrorMessage_when_assertionFails() {
        assertThatThrownBy(() ->
                new ContextAssert(CONTEXT, reporters)
                        .forQuestion(QUESTION)
                        .evaluatedBy(new StubRagJudge(FAILING_VERDICT, FAILING_VERDICT))
                        .hasRelevanceScore(THRESHOLD)
        ).isInstanceOf(AssertionError.class)
                .hasMessageContaining(String.valueOf(LOW));
    }

    @Test
    void should_notifyReporter_when_assertionPasses() {
        new ContextAssert(CONTEXT, reporters)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT))
                .hasRelevanceScore(THRESHOLD);

        assertThat(captured).hasSize(1);
        assertThat(captured.get(0).passed()).isTrue();
    }

    @Test
    void should_notifyReporter_when_assertionFails() {
        assertThatThrownBy(() ->
                new ContextAssert(CONTEXT, reporters)
                        .forQuestion(QUESTION)
                        .evaluatedBy(new StubRagJudge(FAILING_VERDICT, FAILING_VERDICT))
                        .hasRelevanceScore(THRESHOLD)
        ).isInstanceOf(AssertionError.class);

        assertThat(captured).hasSize(1);
        assertThat(captured.get(0).passed()).isFalse();
    }

    @Test
    void should_allowChaining_when_hasRelevanceScoreAndRejectionBothPass() {
        new ContextAssert(CONTEXT, reporters)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT, PASSING_VERDICT))
                .hasRelevanceScore(THRESHOLD)
                .correctlyRefusedToAnswer(THRESHOLD);
    }

    // --- correctlyRefusedToAnswer ---

    @Test
    void should_pass_when_refusalIsJustifiedByInsufficientContext() {
        new ContextAssert(CONTEXT, reporters)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT, PASSING_VERDICT))
                .correctlyRefusedToAnswer(THRESHOLD);
    }

    @Test
    void should_fail_when_refusalIsUnjustifiedGivenRichContext() {
        assertThatThrownBy(() ->
                new ContextAssert(CONTEXT, reporters)
                        .forQuestion(QUESTION)
                        .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT, FAILING_VERDICT))
                        .correctlyRefusedToAnswer(THRESHOLD)
        ).isInstanceOf(AssertionError.class);
    }

    @Test
    void should_throwAssertionError_when_rejectionScoreIsBelowThreshold() {
        assertThatThrownBy(() ->
                new ContextAssert(CONTEXT, reporters)
                        .forQuestion(QUESTION)
                        .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT, FAILING_VERDICT))
                        .correctlyRefusedToAnswer(THRESHOLD)
        ).isInstanceOf(AssertionError.class)
                .hasMessageContaining(String.valueOf(LOW));
    }

    @Test
    void should_reportRejectionAssertion_when_correctlyRefusedToAnswerPasses() {
        new ContextAssert(CONTEXT, reporters)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT, PASSING_VERDICT))
                .correctlyRefusedToAnswer(THRESHOLD);

        assertThat(captured).hasSize(1);
        assertThat(captured.get(0).assertionType()).isEqualTo("REJECTION");
        assertThat(captured.get(0).passed()).isTrue();
    }

    // --- isSafeFromPromptInjection ---

    @Test
    void should_pass_when_contextContainsNoInjectionAttempt() {
        new ContextAssert(CONTEXT, reporters)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT, PASSING_VERDICT))
                .isSafeFromPromptInjection(THRESHOLD);
    }

    @Test
    void should_fail_when_contextContainsInjectionAttempt() {
        assertThatThrownBy(() ->
                new ContextAssert(CONTEXT, reporters)
                        .forQuestion(QUESTION)
                        .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT, FAILING_VERDICT))
                        .isSafeFromPromptInjection(THRESHOLD)
        ).isInstanceOf(AssertionError.class)
                .hasMessageContaining(String.valueOf(LOW));
    }

    @Test
    void should_reportPromptInjectionAssertion_when_contextIsSafe() {
        new ContextAssert(CONTEXT, reporters)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT, PASSING_VERDICT))
                .isSafeFromPromptInjection(THRESHOLD);

        assertThat(captured).hasSize(1);
        assertThat(captured.get(0).assertionType()).isEqualTo("PROMPT_INJECTION");
        assertThat(captured.get(0).passed()).isTrue();
    }

    // --- hasNoPIILeak ---

    @Test
    void should_pass_when_contextContainsNoPII() {
        new ContextAssert(CONTEXT, reporters)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT, PASSING_VERDICT))
                .hasNoPIILeak(THRESHOLD);
    }

    @Test
    void should_fail_when_contextExposesPersonalData() {
        assertThatThrownBy(() ->
                new ContextAssert(CONTEXT, reporters)
                        .forQuestion(QUESTION)
                        .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT, FAILING_VERDICT))
                        .hasNoPIILeak(THRESHOLD)
        ).isInstanceOf(AssertionError.class)
                .hasMessageContaining(String.valueOf(LOW));
    }

    @Test
    void should_reportPIILeakAssertion_when_contextIsClean() {
        new ContextAssert(CONTEXT, reporters)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT, PASSING_VERDICT))
                .hasNoPIILeak(THRESHOLD);

        assertThat(captured).hasSize(1);
        assertThat(captured.get(0).assertionType()).isEqualTo("PII_LEAK");
        assertThat(captured.get(0).passed()).isTrue();
    }

    // --- hasContextRecall ---

    private static final ReferenceAnswer REFERENCE =
            new ReferenceAnswer("Paris is the capital of France and a major European city.");

    @Test
    void should_pass_when_contextCoversAllReferenceStatements() {
        new ContextAssert(CONTEXT, reporters)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT))
                .hasContextRecall(REFERENCE, THRESHOLD);
    }

    @Test
    void should_fail_when_contextMissesCriticalReferenceStatements() {
        assertThatThrownBy(() ->
                new ContextAssert(CONTEXT, reporters)
                        .forQuestion(QUESTION)
                        .evaluatedBy(new StubRagJudge(FAILING_VERDICT, FAILING_VERDICT))
                        .hasContextRecall(REFERENCE, THRESHOLD)
        ).isInstanceOf(AssertionError.class)
                .hasMessageContaining(String.valueOf(LOW));
    }

    @Test
    void should_reportContextRecallAssertion_when_assertionPasses() {
        new ContextAssert(CONTEXT, reporters)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT))
                .hasContextRecall(REFERENCE, THRESHOLD);

        assertThat(captured).hasSize(1);
        assertThat(captured.get(0).assertionType()).isEqualTo("CONTEXT_RECALL");
        assertThat(captured.get(0).passed()).isTrue();
    }

    @Test
    void should_reportContextRecallAssertion_when_assertionFails() {
        assertThatThrownBy(() ->
                new ContextAssert(CONTEXT, reporters)
                        .forQuestion(QUESTION)
                        .evaluatedBy(new StubRagJudge(FAILING_VERDICT, FAILING_VERDICT))
                        .hasContextRecall(REFERENCE, THRESHOLD)
        ).isInstanceOf(AssertionError.class);

        assertThat(captured).hasSize(1);
        assertThat(captured.get(0).assertionType()).isEqualTo("CONTEXT_RECALL");
        assertThat(captured.get(0).passed()).isFalse();
    }

    // --- hasContextPrecision ---

    @Test
    void should_pass_when_contextPrecisionScoreIsAboveThreshold() {
        new ContextAssert(CONTEXT, reporters)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT))
                .hasContextPrecision(THRESHOLD);
    }

    @Test
    void should_fail_when_contextPrecisionScoreIsBelowThreshold() {
        assertThatThrownBy(() ->
                new ContextAssert(CONTEXT, reporters)
                        .forQuestion(QUESTION)
                        .evaluatedBy(new StubRagJudge(FAILING_VERDICT, FAILING_VERDICT))
                        .hasContextPrecision(THRESHOLD)
        ).isInstanceOf(AssertionError.class)
                .hasMessageContaining(String.valueOf(LOW));
    }

    @Test
    void should_reportContextPrecisionAssertion_when_assertionPasses() {
        new ContextAssert(CONTEXT, reporters)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT))
                .hasContextPrecision(THRESHOLD);

        assertThat(captured).hasSize(1);
        assertThat(captured.get(0).assertionType()).isEqualTo("CONTEXT_PRECISION");
        assertThat(captured.get(0).passed()).isTrue();
    }

    @Test
    void should_reportContextPrecisionAssertion_when_assertionFails() {
        assertThatThrownBy(() ->
                new ContextAssert(CONTEXT, reporters)
                        .forQuestion(QUESTION)
                        .evaluatedBy(new StubRagJudge(FAILING_VERDICT, FAILING_VERDICT))
                        .hasContextPrecision(THRESHOLD)
        ).isInstanceOf(AssertionError.class);

        assertThat(captured).hasSize(1);
        assertThat(captured.get(0).assertionType()).isEqualTo("CONTEXT_PRECISION");
        assertThat(captured.get(0).passed()).isFalse();
    }

    @Test
    void should_returnThis_when_chainingAllContextSafetyAssertions() {
        ContextAssert result = new ContextAssert(CONTEXT, reporters)
                .forQuestion(QUESTION)
                .evaluatedBy(new StubRagJudge(PASSING_VERDICT, PASSING_VERDICT, PASSING_VERDICT))
                .correctlyRefusedToAnswer(THRESHOLD)
                .isSafeFromPromptInjection(THRESHOLD)
                .hasNoPIILeak(THRESHOLD)
                .hasContextRecall(REFERENCE, THRESHOLD)
                .hasContextPrecision(THRESHOLD);
        assertThat(result).isNotNull();
    }
}
