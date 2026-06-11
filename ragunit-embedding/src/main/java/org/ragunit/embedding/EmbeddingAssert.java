package org.ragunit.embedding;

import org.ragunit.core.domain.Answer;

import java.util.Objects;

/**
 * Entry point for embedding-based assertions.
 *
 * <pre>{@code
 * EmbeddingProvider provider = new OllamaEmbeddingProvider("nomic-embed-text");
 *
 * // Semantic similarity: answer vs reference
 * EmbeddingAssert.assertThatAnswer(answer)
 *         .comparedTo(reference)
 *         .using(provider)
 *         .hasSemanticSimilarity(0.85);
 *
 * // Answer relevancy: answer vs question (embedding-based)
 * EmbeddingAssert.assertThatAnswer(answer)
 *         .forQuestion(question)
 *         .using(provider)
 *         .isRelevantToQuestion(0.70);
 * }</pre>
 */
public final class EmbeddingAssert {

    private EmbeddingAssert() {
    }

    /**
     * Creates a fluent assertion builder for the given answer.
     *
     * @param answer the generated answer to evaluate (must not be null)
     * @return a new {@link EmbeddingAnswerAssert} builder
     */
    public static EmbeddingAnswerAssert assertThatAnswer(Answer answer) {
        return new EmbeddingAnswerAssert(Objects.requireNonNull(answer, "answer"));
    }
}
