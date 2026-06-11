package org.ragunit.core.assertion;

import org.junit.jupiter.api.Test;
import org.ragunit.core.domain.Answer;
import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.Score;
import org.ragunit.core.domain.Verdict;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests for the public entry point RagAssert — verifies both static factory methods. */
class RagAssertTest {

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

    @Test
    void should_failAssertion_when_usingAssertThatContextWithLowScore() {
        assertThatThrownBy(() ->
                RagAssert.assertThatContext(CONTEXT)
                        .forQuestion(QUESTION)
                        .evaluatedBy(new StubRagJudge(FAILING_VERDICT, FAILING_VERDICT))
                        .hasRelevanceScore(THRESHOLD)
        ).isInstanceOf(AssertionError.class);
    }

    @Test
    void should_failAssertion_when_usingAssertThatAnswerWithLowScore() {
        assertThatThrownBy(() ->
                RagAssert.assertThatAnswer(ANSWER)
                        .givenContext(CONTEXT)
                        .forQuestion(QUESTION)
                        .evaluatedBy(new StubRagJudge(PASSING_VERDICT, FAILING_VERDICT))
                        .isFaithfulToContext(THRESHOLD)
        ).isInstanceOf(AssertionError.class);
    }
}
