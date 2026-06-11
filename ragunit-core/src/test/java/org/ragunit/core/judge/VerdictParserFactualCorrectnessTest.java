package org.ragunit.core.judge;

import org.junit.jupiter.api.Test;
import org.ragunit.core.domain.FactualCorrectnessVerdict;

import static org.assertj.core.api.Assertions.assertThat;

/** Tests for {@link VerdictParser#parseFactualCorrectness(String, String)}. */
class VerdictParserFactualCorrectnessTest {

    private static final String MODEL = "test-model";

    @Test
    void should_parsePrecisionRecallAndF1_when_validJson() {
        String response = "{\"score\": 0.75, \"precision\": 0.80, \"recall\": 0.70, "
                + "\"rationale\": \"Good overlap.\", \"statements\": []}";

        FactualCorrectnessVerdict verdict = VerdictParser.parseFactualCorrectness(response, MODEL);

        assertThat(verdict.f1().value()).isEqualTo(0.75);
        assertThat(verdict.precision().value()).isEqualTo(0.80);
        assertThat(verdict.recall().value()).isEqualTo(0.70);
        assertThat(verdict.rationale()).isEqualTo("Good overlap.");
        assertThat(verdict.model()).isEqualTo(MODEL);
    }

    @Test
    void should_returnZeroScores_when_fieldsAreMissing() {
        String response = "{\"rationale\": \"Could not parse.\"}";

        FactualCorrectnessVerdict verdict = VerdictParser.parseFactualCorrectness(response, MODEL);

        assertThat(verdict.f1().value()).isEqualTo(0.0);
        assertThat(verdict.precision().value()).isEqualTo(0.0);
        assertThat(verdict.recall().value()).isEqualTo(0.0);
    }

    @Test
    void should_clampScoresToValidRange_when_llmExceedsOne() {
        String response = "{\"score\": 1.5, \"precision\": 2.0, \"recall\": -0.3, "
                + "\"rationale\": \"out of range\", \"statements\": []}";

        FactualCorrectnessVerdict verdict = VerdictParser.parseFactualCorrectness(response, MODEL);

        assertThat(verdict.f1().value()).isEqualTo(1.0);
        assertThat(verdict.precision().value()).isEqualTo(1.0);
        assertThat(verdict.recall().value()).isEqualTo(0.0);
    }

    @Test
    void should_returnRawResponseAsRationale_when_rationaleFieldMissing() {
        String response = "{\"score\": 0.5, \"precision\": 0.6, \"recall\": 0.4}";

        FactualCorrectnessVerdict verdict = VerdictParser.parseFactualCorrectness(response, MODEL);

        assertThat(verdict.rationale()).isEqualTo(response.trim());
    }

    @Test
    void should_parseIntegerScores_when_llmOmitsDecimalPoint() {
        String response = "{\"score\": 1, \"precision\": 1, \"recall\": 0, "
                + "\"rationale\": \"Perfect precision, no recall.\", \"statements\": []}";

        FactualCorrectnessVerdict verdict = VerdictParser.parseFactualCorrectness(response, MODEL);

        assertThat(verdict.f1().value()).isEqualTo(1.0);
        assertThat(verdict.precision().value()).isEqualTo(1.0);
        assertThat(verdict.recall().value()).isEqualTo(0.0);
    }
}
