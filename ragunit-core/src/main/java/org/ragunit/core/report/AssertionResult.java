package org.ragunit.core.report;

import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.Verdict;

/**
 * Immutable snapshot of a completed assertion, passed to every registered {@link RagReporter}.
 *
 * <p>Contains all the information needed for reporting: what was evaluated, the result,
 * the threshold used, and whether the assertion passed.
 *
 * @param assertionType the metric name (e.g. {@code "FAITHFULNESS"})
 * @param question      the question that was evaluated
 * @param verdict       the judge's evaluation result
 * @param threshold     the minimum acceptable score that was asserted
 * @param passed        whether the verdict's score met or exceeded the threshold
 */
public record AssertionResult(
        String assertionType,
        Question question,
        Verdict verdict,
        double threshold,
        boolean passed) {
}
