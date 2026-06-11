package org.ragunit.core.domain;

import java.util.Objects;

/**
 * An atomic claim extracted from an {@link Answer}, with a verdict on whether it is
 * supported by the retrieved context.
 *
 * <p>Used by {@link Verdict} to provide a RAGAS-style decomposition:
 * each statement in the answer is individually verified against the context,
 * making the {@link Verdict#computedScore()} reproducible and auditable.
 *
 * @param text      the text of the atomic claim
 * @param supported whether the claim is supported by the retrieved context
 */
public record Statement(String text, boolean supported) {

    /** Validates that text is not null. */
    public Statement {
        Objects.requireNonNull(text, "text");
    }
}
