package org.ragunit.core.testset;

import java.io.Serial;

/**
 * Thrown when a {@link TestsetGenerator} fails to generate test cases due to a
 * network error, LLM unavailability, or an unrecoverable parsing failure.
 */
public final class TestsetGeneratorException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates a TestsetGeneratorException with a message and a cause.
     *
     * @param message description of the failure
     * @param cause   the underlying exception
     */
    public TestsetGeneratorException(String message, Throwable cause) {
        super(message, cause);
    }
}
