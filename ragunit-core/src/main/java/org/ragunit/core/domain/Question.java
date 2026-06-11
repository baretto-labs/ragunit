package org.ragunit.core.domain;

import java.util.Objects;

/**
 * The user's input query submitted to the RAG pipeline. Must not be blank.
 *
 * @param text the query text; must not be null or blank
 */
public record Question(String text) {

    /** Validates that the question is not null and not blank. */
    public Question {
        Objects.requireNonNull(text, "text");
        if (text.isBlank()) {
            throw new IllegalArgumentException("Question text must not be blank");
        }
    }
}
