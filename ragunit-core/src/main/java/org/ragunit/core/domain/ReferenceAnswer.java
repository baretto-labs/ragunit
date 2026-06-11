package org.ragunit.core.domain;

import java.util.Objects;

/**
 * The ground-truth answer used to evaluate {@code Context Recall}.
 *
 * <p>A {@code ReferenceAnswer} is a human-authored or curated correct answer
 * to a question. The judge decomposes it into claims and verifies each claim
 * is supported by the retrieved context. Score = covered claims / total claims.
 *
 * <p>This is the only RAGUnit metric requiring a ground truth — all other metrics
 * are reference-free and evaluate the pipeline's own outputs.
 *
 * @param text the full text of the reference answer (must be non-blank)
 */
public record ReferenceAnswer(String text) {

    /**
     * Validates that text is non-null and non-blank.
     *
     * @throws IllegalArgumentException if {@code text} is blank
     */
    public ReferenceAnswer {
        Objects.requireNonNull(text, "text");
        if (text.isBlank()) {
            throw new IllegalArgumentException("ReferenceAnswer cannot be blank");
        }
    }
}
