package org.ragunit.core.judge;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link Criterion}: the named evaluation question asked to a judge.
 */
class CriterionTest {

    @Test
    void should_exposeNameAndInstruction_when_createdViaFactory() {
        Criterion criterion = Criterion.of("conciseness", "Is the summary concise?");

        assertThat(criterion.name()).isEqualTo("conciseness");
        assertThat(criterion.instruction()).isEqualTo("Is the summary concise?");
    }

    @Test
    void should_throwNullPointerException_when_nameIsNull() {
        assertThatThrownBy(() -> Criterion.of(null, "instruction"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throwIllegalArgumentException_when_instructionIsBlank() {
        assertThatThrownBy(() -> Criterion.of("name", "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_beUsableAsCriterion_when_metricTypeIsPassed() {
        Criterion criterion = MetricType.GENERATION;

        assertThat(criterion.name()).isEqualTo("GENERATION");
        assertThat(criterion.instruction()).isNotBlank();
    }

    @Test
    void should_haveNonBlankInstruction_when_anyMetricTypeIsUsed() {
        for (MetricType metric : MetricType.values()) {
            assertThat(metric.instruction())
                    .as("instruction of %s", metric)
                    .isNotBlank();
        }
    }
}
