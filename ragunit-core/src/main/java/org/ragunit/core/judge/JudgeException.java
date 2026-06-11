package org.ragunit.core.judge;

import java.io.Serial;

/**
 * Thrown when a {@link RagJudge} fails to produce a {@link org.ragunit.core.domain.Verdict}.
 *
 * <p>Wraps infrastructure errors (network, timeout, parse failure) so that callers
 * never see raw {@code IOException} or {@code InterruptedException} leaking from the HTTP layer.
 */
public final class JudgeException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a JudgeException with a message and a cause.
     *
     * @param message description of what failed
     * @param cause   the underlying infrastructure exception
     */
    public JudgeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a JudgeException with a message only (no cause).
     *
     * @param message description of what failed
     */
    public JudgeException(String message) {
        super(message);
    }
}
