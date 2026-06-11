package org.ragunit.core.assertion;

import org.ragunit.core.domain.Score;
import org.ragunit.core.domain.ScoreStatistics;
import org.ragunit.core.domain.Verdict;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Runs a judge evaluation N times and aggregates the verdicts into
 * {@link ScoreStatistics}, implementing the variance-control contract shared
 * by {@link AnswerAssert} and {@link ContextAssert}.
 *
 * <p>An assertion passes only when the mean score reaches the threshold AND
 * the standard deviation stays within the configured bound — a high mean from
 * an erratic judge is noise, not a measurement.
 */
final class RepeatedEvaluation {

    /** Maximum justification length quoted in AssertionError messages. */
    private static final int MAX_JUSTIFICATION_CHARS = 500;

    private final List<Verdict> verdicts;
    private final ScoreStatistics statistics;

    private RepeatedEvaluation(List<Verdict> verdicts) {
        this.verdicts = verdicts;
        this.statistics = ScoreStatistics.of(verdicts.stream().map(Verdict::score).toList());
    }

    static RepeatedEvaluation run(int runs, Supplier<Verdict> evaluation) {
        List<Verdict> verdicts = new ArrayList<>();
        for (int i = 0; i < runs; i++) {
            verdicts.add(evaluation.get());
        }
        return new RepeatedEvaluation(verdicts);
    }

    boolean passes(double threshold, double maxStddev) {
        return !statistics.isBelow(threshold) && !statistics.isUnstable(maxStddev);
    }

    /**
     * The verdict reported to observers: the single verdict when run once,
     * otherwise the last verdict re-scored with the mean over all runs.
     */
    Verdict aggregatedVerdict() {
        Verdict last = verdicts.get(verdicts.size() - 1);
        if (verdicts.size() == 1) {
            return last;
        }
        return new Verdict(new Score(statistics.mean()), last.rationale(), last.model(),
                last.statements(), last.chunkVerdicts(), last.promptUsed(), last.rawResponse());
    }

    String failureMessage(String label, double threshold, double maxStddev) {
        List<String> failures = new ArrayList<>();
        if (statistics.isBelow(threshold)) {
            failures.add(belowThresholdMessage(label, threshold));
        }
        if (statistics.isUnstable(maxStddev)) {
            failures.add("%s: judge too unstable: stddev %s over %d runs exceeds max %s (mean %s)"
                    .formatted(label, statistics.stddev(), statistics.runs(), maxStddev,
                            statistics.mean()));
        }
        return String.join(" AND ", failures) + justificationSuffix();
    }

    private String justificationSuffix() {
        String justification = verdicts.get(verdicts.size() - 1).rationale().trim();
        if (justification.isEmpty()) {
            return "";
        }
        if (justification.length() > MAX_JUSTIFICATION_CHARS) {
            justification = justification.substring(0, MAX_JUSTIFICATION_CHARS) + "…";
        }
        return " — judge justification: \"%s\"".formatted(justification);
    }

    private String belowThresholdMessage(String label, double threshold) {
        if (statistics.runs() == 1) {
            return label + " score " + statistics.mean() + " is below threshold " + threshold;
        }
        return "%s mean score %s over %d runs is below threshold %s (stddev %s)"
                .formatted(label, statistics.mean(), statistics.runs(), threshold,
                        statistics.stddev());
    }
}
