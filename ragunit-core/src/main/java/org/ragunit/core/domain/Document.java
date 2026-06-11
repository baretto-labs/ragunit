package org.ragunit.core.domain;

import java.util.Objects;

/**
 * A single chunk of text retrieved by the Retriever, forming part of the {@code Context}.
 *
 * @param content the document text; must not be null
 */
public record Document(String content) {

    /** Validates that content is not null. */
    public Document {
        Objects.requireNonNull(content, "content");
    }
}
