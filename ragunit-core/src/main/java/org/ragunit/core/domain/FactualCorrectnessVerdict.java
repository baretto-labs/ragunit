package org.ragunit.core.domain;

import java.util.Objects;

/**
 * The result of a FactualCorrectness evaluation: claim-level comparison between an
 * {@link Answer} and a {@link ReferenceAnswer}.
 *
 * <p>Three complementary scores are produced in a single judge call:
 * <ul>
 *   <li>{@link #f1()} — harmonic mean of precision and recall; the primary threshold metric</li>
 *   <li>{@link #precision()} — fraction of Answer claims supported by the Reference</li>
 *   <li>{@link #recall()} — fraction of Reference claims covered by the Answer</li>
 * </ul>
 *
 * <p>A pipeline can be faithful (no hallucination) but score low on FactualCorrectness if
 * the retrieved context itself contains incorrect or incomplete information.
 *
 * @param f1        harmonic mean of precision and recall; the primary threshold metric
 * @param precision fraction of Answer claims supported by the ReferenceAnswer
 * @param recall    fraction of ReferenceAnswer claims covered by the Answer
 * @param rationale the judge's human-readable explanation
 * @param model     the identifier of the model that produced this verdict
 */
public record FactualCorrectnessVerdict(Score f1, Score precision, Score recall,
                                        String rationale, String model) {

    /** Validates all fields. */
    public FactualCorrectnessVerdict {
        Objects.requireNonNull(f1, "f1");
        Objects.requireNonNull(precision, "precision");
        Objects.requireNonNull(recall, "recall");
        Objects.requireNonNull(rationale, "rationale");
        Objects.requireNonNull(model, "model");
    }

    /**
     * Returns true if the F1 score meets or exceeds the given threshold.
     *
     * @param threshold minimum acceptable F1 score in [0.0, 1.0]
     * @return true when {@code f1.value() >= threshold}
     */
    public boolean isF1AboveThreshold(double threshold) {
        return f1.value() >= threshold;
    }

    /**
     * Returns true if the precision score meets or exceeds the given threshold.
     *
     * @param threshold minimum acceptable precision in [0.0, 1.0]
     * @return true when {@code precision.value() >= threshold}
     */
    public boolean isPrecisionAboveThreshold(double threshold) {
        return precision.value() >= threshold;
    }

    /**
     * Returns true if the recall score meets or exceeds the given threshold.
     *
     * @param threshold minimum acceptable recall in [0.0, 1.0]
     * @return true when {@code recall.value() >= threshold}
     */
    public boolean isRecallAboveThreshold(double threshold) {
        return recall.value() >= threshold;
    }
}
