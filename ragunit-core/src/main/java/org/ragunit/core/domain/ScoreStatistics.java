package org.ragunit.core.domain;

import java.util.List;

/**
 * Aggregated statistics over repeated judge runs of the same evaluation:
 * mean score, population standard deviation, and number of runs.
 *
 * <p>A single LLM-judge call carries inherent variance (typically ±10–15%).
 * Averaging over N runs and bounding the standard deviation turns a noisy
 * score into a defensible measurement. Produced by the assertion builders
 * when {@code withRuns(n)} is configured.
 *
 * @param mean   the arithmetic mean of the scores, in [0.0, 1.0]
 * @param stddev the population standard deviation of the scores
 * @param runs   the number of judge runs aggregated (≥ 1)
 */
public record ScoreStatistics(double mean, double stddev, int runs) {

    /**
     * Default stability bound: a judge whose scores deviate more than this
     * across runs is considered too unstable for a meaningful assertion.
     */
    public static final double DEFAULT_MAX_STDDEV = 0.15;

    /**
     * Computes mean and population standard deviation from a list of scores.
     *
     * <p>Population (not sample) standard deviation is used so a single run
     * yields {@code stddev = 0.0} instead of being undefined.
     *
     * @param scores one score per judge run; must not be empty
     * @return the aggregated statistics
     * @throws IllegalArgumentException if {@code scores} is empty
     */
    public static ScoreStatistics of(List<Score> scores) {
        if (scores.isEmpty()) {
            throw new IllegalArgumentException("scores must not be empty");
        }
        double mean = scores.stream().mapToDouble(Score::value).average().orElseThrow();
        double variance = scores.stream()
                .mapToDouble(score -> Math.pow(score.value() - mean, 2))
                .average().orElseThrow();
        return new ScoreStatistics(mean, Math.sqrt(variance), scores.size());
    }

    /**
     * Returns true when the mean score does not reach the threshold.
     *
     * @param threshold the minimum acceptable mean score
     * @return true when {@code mean < threshold}
     */
    public boolean isBelow(double threshold) {
        return mean < threshold;
    }

    /**
     * Returns true when the judge is too unstable for the assertion to be meaningful.
     *
     * @param maxStddev the maximum acceptable standard deviation
     * @return true when {@code stddev > maxStddev}
     */
    public boolean isUnstable(double maxStddev) {
        return stddev > maxStddev;
    }
}
