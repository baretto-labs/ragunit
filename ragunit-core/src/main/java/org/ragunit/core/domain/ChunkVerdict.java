package org.ragunit.core.domain;

import java.util.Objects;

/**
 * The per-chunk relevance verdict produced by a Context Precision evaluation.
 *
 * <p>Each retrieved {@link Document} is assessed independently: is it relevant
 * to the question? The {@code rank} reflects its position in the retrieval result
 * (1 = first returned, highest priority).
 *
 * <p>Used to compute the RAGAS Average Precision score, which rewards pipelines
 * that put relevant chunks at the top of the retrieved list.
 *
 * @param chunk    the retrieved document that was evaluated
 * @param relevant whether this chunk is relevant to the question
 * @param rank     1-based position in the retrieval result list
 */
public record ChunkVerdict(Document chunk, boolean relevant, int rank) {

    /**
     * Validates that rank is at least 1.
     *
     * @throws IllegalArgumentException if {@code rank} is less than 1
     */
    public ChunkVerdict {
        Objects.requireNonNull(chunk, "chunk");
        if (rank < 1) {
            throw new IllegalArgumentException("rank must be >= 1, got: " + rank);
        }
    }
}
