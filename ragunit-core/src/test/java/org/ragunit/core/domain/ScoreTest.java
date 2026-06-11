package org.ragunit.core.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests for the Score value object invariant: value must be in [0.0, 1.0]. */
class ScoreTest {

    private static final double BELOW_MIN = -0.01;
    private static final double ABOVE_MAX = 1.01;
    private static final double MID_RANGE = 0.5;
    private static final double MIN = 0.0;
    private static final double MAX = 1.0;

    @Test
    void should_rejectScore_when_valueIsNegative() {
        assertThatThrownBy(() -> new Score(BELOW_MIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(BELOW_MIN));
    }

    @Test
    void should_rejectScore_when_valueIsGreaterThanOne() {
        assertThatThrownBy(() -> new Score(ABOVE_MAX))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(ABOVE_MAX));
    }

    @Test
    void should_createScore_when_valueIsZero() {
        assertThat(new Score(MIN).value()).isEqualTo(MIN);
    }

    @Test
    void should_createScore_when_valueIsOne() {
        assertThat(new Score(MAX).value()).isEqualTo(MAX);
    }

    @Test
    void should_createScore_when_valueIsInRange() {
        assertThat(new Score(MID_RANGE).value()).isEqualTo(MID_RANGE);
    }
}
