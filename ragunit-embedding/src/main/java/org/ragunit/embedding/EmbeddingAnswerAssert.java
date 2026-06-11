package org.ragunit.embedding;

import org.ragunit.core.domain.Answer;
import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.ReferenceAnswer;

import java.util.Objects;

/**
 * Fluent assertion builder for embedding-based metrics.
 *
 * <p>Obtain an instance via {@link EmbeddingAssert#assertThatAnswer(Answer)}.
 *
 * <p>Supports two comparison modes:
 * <ul>
 *   <li>{@link #comparedTo(ReferenceAnswer)} + {@link #hasSemanticSimilarity(double)}
 *       — measures how close the answer is to the ground-truth (RAGAS Semantic Similarity)</li>
 *   <li>{@link #forQuestion(Question)} + {@link #isRelevantToQuestion(double)}
 *       — measures how well the answer addresses the question (embedding-based AnswerRelevancy)</li>
 * </ul>
 */
public final class EmbeddingAnswerAssert {

    private final Answer answer;
    private ReferenceAnswer reference;
    private Question question;
    private EmbeddingProvider provider;

    /**
     * Creates a new EmbeddingAnswerAssert for the given answer.
     *
     * @param answer the generated answer to evaluate; must not be null
     */
    public EmbeddingAnswerAssert(Answer answer) {
        this.answer = Objects.requireNonNull(answer, "answer");
    }

    /**
     * Sets the ground-truth reference answer for semantic similarity comparison.
     *
     * @param reference the human-authored reference answer; must not be null
     * @return this, for chaining
     */
    public EmbeddingAnswerAssert comparedTo(ReferenceAnswer reference) {
        this.reference = Objects.requireNonNull(reference, "reference");
        return this;
    }

    /**
     * Sets the question for embedding-based relevancy comparison.
     *
     * @param question the original user question; must not be null
     * @return this, for chaining
     */
    public EmbeddingAnswerAssert forQuestion(Question question) {
        this.question = Objects.requireNonNull(question, "question");
        return this;
    }

    /**
     * Sets the embedding provider to use for computing vectors.
     *
     * @param provider the embedding backend; must not be null
     * @return this, for chaining
     */
    public EmbeddingAnswerAssert using(EmbeddingProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
        return this;
    }

    /**
     * Asserts that the cosine similarity between the answer and the reference answer
     * meets the given threshold (RAGAS Semantic Similarity).
     *
     * @param threshold minimum acceptable similarity in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the similarity is below the threshold
     */
    public EmbeddingAnswerAssert hasSemanticSimilarity(double threshold) {
        Objects.requireNonNull(provider, "Call using() before asserting");
        Objects.requireNonNull(reference, "Call comparedTo() before calling hasSemanticSimilarity()");
        float[] answerVec = provider.embed(answer.text());
        float[] referenceVec = provider.embed(reference.text());
        double similarity = CosineSimilarity.compute(answerVec, referenceVec);
        if (similarity < threshold) {
            throw new AssertionError(
                    "Semantic similarity " + similarity + " is below threshold " + threshold);
        }
        return this;
    }

    /**
     * Asserts that the cosine similarity between the answer and the question
     * meets the given threshold (embedding-based Answer Relevancy).
     *
     * @param threshold minimum acceptable similarity in [0.0, 1.0]
     * @return this, for chaining
     * @throws AssertionError if the similarity is below the threshold
     */
    public EmbeddingAnswerAssert isRelevantToQuestion(double threshold) {
        Objects.requireNonNull(provider, "Call using() before asserting");
        Objects.requireNonNull(question, "Call forQuestion() before calling isRelevantToQuestion()");
        float[] answerVec = provider.embed(answer.text());
        float[] questionVec = provider.embed(question.text());
        double similarity = CosineSimilarity.compute(answerVec, questionVec);
        if (similarity < threshold) {
            throw new AssertionError(
                    "Answer relevancy (embedding) " + similarity + " is below threshold " + threshold);
        }
        return this;
    }
}
