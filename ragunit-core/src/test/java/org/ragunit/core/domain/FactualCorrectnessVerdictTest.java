package org.ragunit.core.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests for {@link FactualCorrectnessVerdict}. */
class FactualCorrectnessVerdictTest {

    private static final String MODEL = "stub";
    private static final String RATIONALE = "3 of 4 claims matched.";

    @Test
    void should_returnTrue_when_f1_meetsThreshold() {
        var verdict = new FactualCorrectnessVerdict(
                new Score(0.80), new Score(0.90), new Score(0.72), RATIONALE, MODEL);
        assertThat(verdict.isF1AboveThreshold(0.75)).isTrue();
    }

    @Test
    void should_returnFalse_when_f1_belowThreshold() {
        var verdict = new FactualCorrectnessVerdict(
                new Score(0.60), new Score(0.90), new Score(0.45), RATIONALE, MODEL);
        assertThat(verdict.isF1AboveThreshold(0.75)).isFalse();
    }

    @Test
    void should_returnTrue_when_precision_meetsThreshold() {
        var verdict = new FactualCorrectnessVerdict(
                new Score(0.80), new Score(0.90), new Score(0.72), RATIONALE, MODEL);
        assertThat(verdict.isPrecisionAboveThreshold(0.90)).isTrue();
    }

    @Test
    void should_returnFalse_when_precision_belowThreshold() {
        var verdict = new FactualCorrectnessVerdict(
                new Score(0.80), new Score(0.90), new Score(0.72), RATIONALE, MODEL);
        assertThat(verdict.isPrecisionAboveThreshold(0.95)).isFalse();
    }

    @Test
    void should_returnFalse_when_recall_belowThreshold() {
        var verdict = new FactualCorrectnessVerdict(
                new Score(0.80), new Score(0.90), new Score(0.50), RATIONALE, MODEL);
        assertThat(verdict.isRecallAboveThreshold(0.60)).isFalse();
    }

    @Test
    void should_throwNPE_when_f1IsNull() {
        assertThatThrownBy(() ->
                new FactualCorrectnessVerdict(null, new Score(0.9), new Score(0.9), RATIONALE, MODEL))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throwNPE_when_rationaleIsNull() {
        assertThatThrownBy(() ->
                new FactualCorrectnessVerdict(new Score(0.8), new Score(0.9), new Score(0.7), null, MODEL))
                .isInstanceOf(NullPointerException.class);
    }
}
