package org.ragunit.core.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests for Verdict: threshold comparison and null guard. */
class VerdictTest {

    private static final double HIGH_SCORE = 0.95;
    private static final double LOW_SCORE = 0.50;
    private static final double THRESHOLD = 0.90;
    private static final String RATIONALE = "The answer is grounded in the context.";
    private static final String MODEL = "llama3:8b";

    @Test
    void should_beFaithful_when_scoreIsAboveThreshold() {
        Verdict verdict = Verdict.of(new Score(HIGH_SCORE), RATIONALE, MODEL);
        assertThat(verdict.isAboveThreshold(THRESHOLD)).isTrue();
    }

    @Test
    void should_beUnfaithful_when_scoreIsBelowThreshold() {
        Verdict verdict = Verdict.of(new Score(LOW_SCORE), RATIONALE, MODEL);
        assertThat(verdict.isAboveThreshold(THRESHOLD)).isFalse();
    }

    @Test
    void should_passThreshold_when_scoreEqualsThreshold() {
        Verdict verdict = Verdict.of(new Score(THRESHOLD), RATIONALE, MODEL);
        assertThat(verdict.isAboveThreshold(THRESHOLD)).isTrue();
    }

    @Test
    void should_rejectVerdict_when_scoreIsNull() {
        assertThatThrownBy(() -> new Verdict(null, RATIONALE, MODEL, List.of(), List.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_rejectVerdict_when_rationaleIsNull() {
        assertThatThrownBy(() -> new Verdict(new Score(HIGH_SCORE), null, MODEL, List.of(), List.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_rejectVerdict_when_modelIsNull() {
        assertThatThrownBy(() -> new Verdict(new Score(HIGH_SCORE), RATIONALE, null, List.of(), List.of()))
                .isInstanceOf(NullPointerException.class);
    }

    // --- computedPrecision (RAGAS Average Precision) ---

    private static final Document DOC_A = new Document("doc A");
    private static final Document DOC_B = new Document("doc B");
    private static final Document DOC_C = new Document("doc C");

    @Test
    void should_returnEmpty_when_chunkVerdictsIsEmpty() {
        Verdict verdict = Verdict.of(new Score(HIGH_SCORE), RATIONALE, MODEL);
        assertThat(verdict.computedPrecision()).isEmpty();
    }

    @Test
    void should_returnOne_when_allChunksAreRelevant() {
        List<ChunkVerdict> chunks = List.of(
                new ChunkVerdict(DOC_A, true, 1),
                new ChunkVerdict(DOC_B, true, 2),
                new ChunkVerdict(DOC_C, true, 3)
        );
        Verdict verdict = new Verdict(new Score(HIGH_SCORE), RATIONALE, MODEL, List.of(), chunks);
        assertThat(verdict.computedPrecision()).hasValue(1.0);
    }

    @Test
    void should_returnZero_when_noChunkIsRelevant() {
        List<ChunkVerdict> chunks = List.of(
                new ChunkVerdict(DOC_A, false, 1),
                new ChunkVerdict(DOC_B, false, 2)
        );
        Verdict verdict = new Verdict(new Score(LOW_SCORE), RATIONALE, MODEL, List.of(), chunks);
        assertThat(verdict.computedPrecision()).hasValue(0.0);
    }

    @Test
    void should_computePrecisionCorrectly_when_chunkVerdictsAreOutOfOrder() {
        // Chunks provided in wrong order: rank 3 first, then rank 1, then rank 2
        // Sorted correctly: rank 1 (relevant), rank 2 (relevant), rank 3 (not relevant)
        // AP = (1/1 + 2/2) / 2 = 1.0
        List<ChunkVerdict> outOfOrder = List.of(
                new ChunkVerdict(DOC_C, false, 3),
                new ChunkVerdict(DOC_A, true, 1),
                new ChunkVerdict(DOC_B, true, 2)
        );
        Verdict verdict = new Verdict(new Score(HIGH_SCORE), RATIONALE, MODEL, List.of(), outOfOrder);
        assertThat(verdict.computedPrecision()).hasValue(1.0);
    }

    @Test
    void should_scoreHigher_when_relevantChunksAreTopRanked() {
        // relevant at rank 1, 2 — irrelevant at rank 3
        List<ChunkVerdict> topRanked = List.of(
                new ChunkVerdict(DOC_A, true, 1),
                new ChunkVerdict(DOC_B, true, 2),
                new ChunkVerdict(DOC_C, false, 3)
        );
        // relevant at rank 2, 3 — irrelevant at rank 1
        List<ChunkVerdict> bottomRanked = List.of(
                new ChunkVerdict(DOC_A, false, 1),
                new ChunkVerdict(DOC_B, true, 2),
                new ChunkVerdict(DOC_C, true, 3)
        );
        Verdict topVerdict = new Verdict(new Score(HIGH_SCORE), RATIONALE, MODEL, List.of(), topRanked);
        Verdict bottomVerdict = new Verdict(new Score(HIGH_SCORE), RATIONALE, MODEL, List.of(), bottomRanked);

        assertThat(topVerdict.computedPrecision().getAsDouble())
                .isGreaterThan(bottomVerdict.computedPrecision().getAsDouble());
    }
}
