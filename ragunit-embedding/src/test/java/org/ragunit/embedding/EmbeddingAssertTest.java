package org.ragunit.embedding;

import org.junit.jupiter.api.Test;
import org.ragunit.core.domain.Answer;
import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.ReferenceAnswer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmbeddingAssertTest {

    // Stub: identical embedding for any text → similarity = 1.0
    private static final EmbeddingProvider IDENTICAL_PROVIDER = text -> new float[]{1.0f, 0.0f};

    // Stub: orthogonal embeddings → similarity = 0.0
    private static final EmbeddingProvider FIRST_CALL = new EmbeddingProvider() {
        private int calls = 0;
        @Override
        public float[] embed(String text) {
            return calls++ == 0 ? new float[]{1.0f, 0.0f} : new float[]{0.0f, 1.0f};
        }
    };

    private static final Answer ANSWER = new Answer("Paris is the capital of France.");
    private static final Question QUESTION = new Question("What is the capital of France?");
    private static final ReferenceAnswer REFERENCE = new ReferenceAnswer("Paris is the capital of France.");

    @Test
    void should_pass_when_answer_is_semantically_similar_to_reference() {
        EmbeddingAssert.assertThatAnswer(ANSWER)
                .comparedTo(REFERENCE)
                .using(IDENTICAL_PROVIDER)
                .hasSemanticSimilarity(1.0);
    }

    @Test
    void should_fail_when_answer_is_semantically_different_from_reference() {
        assertThatThrownBy(() ->
                EmbeddingAssert.assertThatAnswer(ANSWER)
                        .comparedTo(REFERENCE)
                        .using(FIRST_CALL)
                        .hasSemanticSimilarity(0.5)
        ).isInstanceOf(AssertionError.class)
         .hasMessageContaining("Semantic similarity");
    }

    @Test
    void should_pass_when_answer_relevant_to_question_by_embedding() {
        EmbeddingAssert.assertThatAnswer(ANSWER)
                .forQuestion(QUESTION)
                .using(IDENTICAL_PROVIDER)
                .isRelevantToQuestion(0.9);
    }

    @Test
    void should_fail_when_answer_not_relevant_to_question() {
        assertThatThrownBy(() ->
                EmbeddingAssert.assertThatAnswer(ANSWER)
                        .forQuestion(QUESTION)
                        .using(new EmbeddingProvider() {
                            private int calls = 0;
                            @Override
                            public float[] embed(String text) {
                                return calls++ == 0 ? new float[]{1.0f, 0.0f} : new float[]{0.0f, 1.0f};
                            }
                        })
                        .isRelevantToQuestion(0.5)
        ).isInstanceOf(AssertionError.class)
         .hasMessageContaining("Answer relevancy");
    }

    @Test
    void should_return_this_for_chaining() {
        EmbeddingAnswerAssert result = EmbeddingAssert.assertThatAnswer(ANSWER)
                .comparedTo(REFERENCE)
                .using(IDENTICAL_PROVIDER)
                .hasSemanticSimilarity(0.5);
        assertThat(result).isNotNull();
    }

    @Test
    void should_throw_when_answer_is_null() {
        assertThatThrownBy(() -> EmbeddingAssert.assertThatAnswer(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throw_when_provider_not_set_before_asserting() {
        assertThatThrownBy(() ->
                EmbeddingAssert.assertThatAnswer(ANSWER)
                        .comparedTo(REFERENCE)
                        .hasSemanticSimilarity(0.5)
        ).isInstanceOf(NullPointerException.class);
    }
}
