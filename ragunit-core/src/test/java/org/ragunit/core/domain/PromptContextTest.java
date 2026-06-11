package org.ragunit.core.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link PromptContext} factories: each evaluation flow exposes
 * exactly the inputs it needs, the rest stay empty.
 */
class PromptContextTest {

    private static final Question QUESTION = new Question("What is the capital of France?");
    private static final List<Document> CONTEXT = List.of(new Document("Paris is the capital of France."));
    private static final Answer ANSWER = new Answer("Paris.");
    private static final ReferenceAnswer REFERENCE = new ReferenceAnswer("The capital of France is Paris.");
    private static final List<ToolCall> TRAJECTORY =
            List.of(new ToolCall("web_search", "capital of France", "Paris"));

    @Test
    void should_haveEmptyAnswerReferenceAndTrajectory_when_builtForRetrieval() {
        PromptContext ctx = PromptContext.forRetrieval(QUESTION, CONTEXT);

        assertThat(ctx.answer()).isEmpty();
        assertThat(ctx.reference()).isEmpty();
        assertThat(ctx.trajectory()).isEmpty();
    }

    @Test
    void should_exposeAnswer_when_builtForGeneration() {
        PromptContext ctx = PromptContext.forGeneration(QUESTION, CONTEXT, ANSWER);

        assertThat(ctx.answer()).contains(ANSWER);
    }

    @Test
    void should_exposeReference_when_builtForContextRecall() {
        PromptContext ctx = PromptContext.forContextRecall(QUESTION, CONTEXT, REFERENCE);

        assertThat(ctx.reference()).contains(REFERENCE);
    }

    @Test
    void should_exposeAnswerAndReference_when_builtForFactualCorrectness() {
        PromptContext ctx = PromptContext.forFactualCorrectness(QUESTION, ANSWER, REFERENCE);

        assertThat(ctx.answer()).contains(ANSWER);
        assertThat(ctx.reference()).contains(REFERENCE);
    }

    @Test
    void should_exposeTrajectoryAndAnswer_when_builtForToolTrajectory() {
        PromptContext ctx = PromptContext.forToolTrajectory(QUESTION, TRAJECTORY, ANSWER);

        assertThat(ctx.trajectory()).isEqualTo(TRAJECTORY);
        assertThat(ctx.answer()).contains(ANSWER);
    }

    @Test
    void should_throwNullPointerException_when_referenceIsNullForContextRecall() {
        assertThatThrownBy(() -> PromptContext.forContextRecall(QUESTION, CONTEXT, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throwNullPointerException_when_trajectoryIsNullForToolTrajectory() {
        assertThatThrownBy(() -> PromptContext.forToolTrajectory(QUESTION, null, ANSWER))
                .isInstanceOf(NullPointerException.class);
    }
}
