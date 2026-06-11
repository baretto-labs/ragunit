package org.ragunit.core.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Carries the evaluation inputs that a {@link org.ragunit.core.judge.JudgePromptTemplate}
 * can use to render a custom prompt.
 *
 * <p>{@code answer} is empty for retrieval-only metrics (e.g. RETRIEVAL, CONTEXT_PRECISION,
 * CONTEXT_RECALL) where no generated answer is available.
 *
 * @param question         the user's original query
 * @param retrievedContext the list of documents retrieved by the Retriever
 * @param answer           the generated answer, or empty if not applicable
 */
public record PromptContext(Question question, List<Document> retrievedContext, Optional<Answer> answer) {

    /** Validates all fields and makes the context list unmodifiable. */
    public PromptContext {
        Objects.requireNonNull(question, "question");
        Objects.requireNonNull(retrievedContext, "retrievedContext");
        Objects.requireNonNull(answer, "answer");
        retrievedContext = List.copyOf(retrievedContext);
    }

    /**
     * Creates a PromptContext for retrieval-only metrics (no answer).
     *
     * @param question the user's original query
     * @param context  the list of retrieved documents
     * @return a PromptContext with an empty answer
     */
    public static PromptContext forRetrieval(Question question, List<Document> context) {
        return new PromptContext(question, context, Optional.empty());
    }

    /**
     * Creates a PromptContext for generation metrics (answer is present).
     *
     * @param question the user's original query
     * @param context  the list of retrieved documents
     * @param answer   the generated answer
     * @return a PromptContext with the answer present
     */
    public static PromptContext forGeneration(Question question, List<Document> context, Answer answer) {
        return new PromptContext(question, context, Optional.of(Objects.requireNonNull(answer, "answer")));
    }
}
