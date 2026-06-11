package org.ragunit.core.judge;

import org.junit.jupiter.api.Test;
import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.Verdict;

import static org.assertj.core.api.Assertions.assertThat;

class VerdictParserTest {

    private static final String MODEL = "llama3:8b";
    private static final double SCORE_085 = 0.85;
    private static final double SCORE_09 = 0.9;

    @Test
    void should_parseScore_when_jsonContainsScoreField() {
        Verdict verdict = VerdictParser.parse("{\"score\": 0.85, \"rationale\": \"OK.\", \"statements\": []}", MODEL);

        assertThat(verdict.score().value()).isEqualTo(SCORE_085);
    }

    @Test
    void should_parseRationale_when_jsonContainsRationaleField() {
        Verdict verdict = VerdictParser.parse("{\"score\": 0.9, \"rationale\": \"Highly relevant.\", \"statements\": []}", MODEL);

        assertThat(verdict.rationale()).isEqualTo("Highly relevant.");
    }

    @Test
    void should_setModelName_when_parsing() {
        Verdict verdict = VerdictParser.parse("{\"score\": 0.5, \"rationale\": \"OK.\", \"statements\": []}", MODEL);

        assertThat(verdict.model()).isEqualTo(MODEL);
    }

    @Test
    void should_parseStatements_when_jsonContainsStatementsArray() {
        String json = "{\"score\": 0.5, \"rationale\": \"Half supported.\", \"statements\": ["
                + "{\"text\": \"Paris is in France.\", \"supported\": true},"
                + "{\"text\": \"Paris has 10M people.\", \"supported\": false}]}";

        Verdict verdict = VerdictParser.parse(json, MODEL);

        assertThat(verdict.statements()).hasSize(2);
        assertThat(verdict.statements().get(0).text()).isEqualTo("Paris is in France.");
        assertThat(verdict.statements().get(0).supported()).isTrue();
        assertThat(verdict.statements().get(1).text()).isEqualTo("Paris has 10M people.");
        assertThat(verdict.statements().get(1).supported()).isFalse();
    }

    @Test
    void should_returnEmptyStatements_when_statementsArrayIsEmpty() {
        Verdict verdict = VerdictParser.parse("{\"score\": 0.9, \"rationale\": \"OK.\", \"statements\": []}", MODEL);

        assertThat(verdict.statements()).isEmpty();
    }

    @Test
    void should_returnZeroScoreAndRawRationale_when_jsonIsMalformed() {
        String raw = "Sorry, I cannot evaluate this.";

        Verdict verdict = VerdictParser.parse(raw, MODEL);

        assertThat(verdict.score().value()).isEqualTo(0.0);
        assertThat(verdict.rationale()).isEqualTo(raw);
        assertThat(verdict.statements()).isEmpty();
    }

    @Test
    void should_computeScore_when_statementsArePresent() {
        String json = "{\"score\": 0.0, \"rationale\": \"Check computed.\", \"statements\": ["
                + "{\"text\": \"A.\", \"supported\": true},"
                + "{\"text\": \"B.\", \"supported\": true},"
                + "{\"text\": \"C.\", \"supported\": false}]}";

        Verdict verdict = VerdictParser.parse(json, MODEL);

        assertThat(verdict.computedScore()).isPresent();
        assertThat(verdict.computedScore().getAsDouble()).isEqualTo(2.0 / 3.0);
    }

    @Test
    void should_returnEmptyComputedScore_when_statementsAreEmpty() {
        Verdict verdict = VerdictParser.parse("{\"score\": 0.9, \"rationale\": \"OK.\", \"statements\": []}", MODEL);

        assertThat(verdict.computedScore()).isEmpty();
    }

    @Test
    void should_clampScoreToOne_when_llmReturnsTooHighValue() {
        Verdict verdict = VerdictParser.parse("{\"score\": 1.5, \"rationale\": \"OK.\", \"statements\": []}", MODEL);

        assertThat(verdict.score().value()).isEqualTo(1.0);
    }

    @Test
    void should_parseScore_when_responseWrappedInMarkdownCodeBlock() {
        String response = "```json\n{\"score\": 0.9, \"rationale\": \"OK.\", \"statements\": []}\n```";

        Verdict verdict = VerdictParser.parse(response, MODEL);

        assertThat(verdict.score().value()).isEqualTo(SCORE_09);
        assertThat(verdict.rationale()).isEqualTo("OK.");
    }

    @Test
    void should_parseUnescapedQuotesInText_when_statementContainsEscapedChars() {
        String json = "{\"score\": 1.0, \"rationale\": \"OK.\", \"statements\": ["
                + "{\"text\": \"He said \\\\\\\"hello\\\\\\\".\", \"supported\": true}]}";

        Verdict verdict = VerdictParser.parse(json, MODEL);

        assertThat(verdict.statements()).hasSize(1);
    }

    @Test
    void should_returnZeroScore_when_jsonMissingScoreField() {
        Verdict verdict = VerdictParser.parse("{\"rationale\": \"OK.\", \"statements\": []}", MODEL);

        assertThat(verdict.score().value()).isEqualTo(0.0);
    }

    // --- 3-arg parse with context (chunks) ---

    private static final Document DOC_1 = new Document("Paris is the capital.");
    private static final Document DOC_2 = new Document("France is in Europe.");
    private static final java.util.List<Document> CONTEXT = java.util.List.of(DOC_1, DOC_2);

    @Test
    void should_returnChunkVerdicts_when_judgeResponseContainsChunksArray() {
        String json = "{\"score\": 0.87, \"rationale\": \"2 of 2 chunks relevant.\","
                + " \"statements\": [],"
                + " \"chunks\": [{\"rank\": 1, \"relevant\": true}, {\"rank\": 2, \"relevant\": false}]}";

        Verdict verdict = VerdictParser.parse(json, MODEL, CONTEXT);

        assertThat(verdict.chunkVerdicts()).hasSize(2);
        assertThat(verdict.chunkVerdicts().get(0).rank()).isEqualTo(1);
        assertThat(verdict.chunkVerdicts().get(0).relevant()).isTrue();
        assertThat(verdict.chunkVerdicts().get(0).chunk()).isEqualTo(DOC_1);
        assertThat(verdict.chunkVerdicts().get(1).rank()).isEqualTo(2);
        assertThat(verdict.chunkVerdicts().get(1).relevant()).isFalse();
        assertThat(verdict.chunkVerdicts().get(1).chunk()).isEqualTo(DOC_2);
    }

    @Test
    void should_returnEmptyChunkVerdicts_when_chunksArrayIsMissing() {
        String json = "{\"score\": 0.9, \"rationale\": \"OK.\", \"statements\": []}";

        Verdict verdict = VerdictParser.parse(json, MODEL, CONTEXT);

        assertThat(verdict.chunkVerdicts()).isEmpty();
    }
}
