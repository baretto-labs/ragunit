package org.ragunit.core.domain;

/**
 * A normalized quality value in [0.0, 1.0] produced by a {@link org.ragunit.core.judge.RagJudge}.
 *
 * @param value the quality score, must be in [0.0, 1.0]
 */
public record Score(double value) {

    /** Validates that the value is within the allowed range. */
    public Score {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(
                    "Score must be in [0.0, 1.0] but was: " + value);
        }
    }
}
