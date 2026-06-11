package org.ragunit.core.report;

/**
 * Observer notified silently after each {@link org.ragunit.core.assertion.RagAssert} assertion.
 *
 * <p>Implementations must never throw — a reporter failure must not interrupt test execution.
 * This is a functional interface: implementations can be provided as lambdas.
 */
@FunctionalInterface
public interface RagReporter {

    /**
     * Called after an assertion completes, whether it passed or failed.
     *
     * @param result the full context of the assertion that just ran
     */
    void report(AssertionResult result);
}
