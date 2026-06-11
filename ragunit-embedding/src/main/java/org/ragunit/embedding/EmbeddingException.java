package org.ragunit.embedding;

import java.io.Serial;

/**
 * Thrown when an {@link EmbeddingProvider} fails to compute an embedding.
 *
 * <p>Wraps network errors, HTTP error responses, or parse failures from the embedding backend.
 */
public final class EmbeddingException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates an EmbeddingException with a message and a cause.
     *
     * @param message description of the failure
     * @param cause   the underlying exception
     */
    public EmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an EmbeddingException with a message and no cause.
     *
     * @param message description of the failure
     */
    public EmbeddingException(String message) {
        super(message);
    }
}
