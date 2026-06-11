package org.ragunit.core.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * The result of a {@link org.ragunit.core.judge.RagJudge} evaluation: a {@link Score},
 * a human-readable rationale, the name of the model that produced it, an optional
 * list of {@link Statement} objects for claim-level decomposition (RAGAS-style faithfulness),
 * and an optional list of {@link ChunkVerdict} objects for per-chunk precision evaluation.
 *
 * <p>When {@code statements} is non-empty, {@link #computedScore()} returns the ratio
 * of supported claims over total claims — reproducible and independent of the LLM's
 * declared score.
 *
 * <p>When {@code chunkVerdicts} is non-empty, {@link #computedPrecision()} returns the
 * RAGAS Average Precision score — rewarding pipelines that rank relevant chunks first.
 *
 * <p>When produced by a real judge, the verdict also carries the exact prompt sent
 * to the LLM ({@code promptUsed}) and the raw LLM response ({@code rawResponse}) —
 * a score has no meaning if the question asked to the judge cannot be inspected.
 *
 * @param score         the normalized quality score in [0.0, 1.0]
 * @param rationale     the judge's human-readable explanation
 * @param model         the identifier of the model that produced this verdict
 * @param statements    claim-level decomposition for faithfulness (may be empty)
 * @param chunkVerdicts per-chunk relevance judgments for context precision (may be empty)
 * @param promptUsed    the exact prompt sent to the judge LLM, or empty if not captured
 * @param rawResponse   the raw LLM response the verdict was parsed from, or empty if not captured
 */
public record Verdict(Score score, String rationale, String model,
                      List<Statement> statements, List<ChunkVerdict> chunkVerdicts,
                      Optional<String> promptUsed, Optional<String> rawResponse) {

    /** Validates all fields and defensive-copies both lists. */
    public Verdict {
        Objects.requireNonNull(score, "score");
        Objects.requireNonNull(rationale, "rationale");
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(statements, "statements");
        Objects.requireNonNull(chunkVerdicts, "chunkVerdicts");
        Objects.requireNonNull(promptUsed, "promptUsed");
        Objects.requireNonNull(rawResponse, "rawResponse");
        statements = List.copyOf(statements);
        chunkVerdicts = List.copyOf(chunkVerdicts);
    }

    /**
     * Creates a verdict without the prompt/response exchange (v0.1-compatible form).
     *
     * @param score         the normalized quality score
     * @param rationale     the judge's explanation
     * @param model         the model identifier
     * @param statements    claim-level decomposition (may be empty)
     * @param chunkVerdicts per-chunk relevance judgments (may be empty)
     */
    public Verdict(Score score, String rationale, String model,
                   List<Statement> statements, List<ChunkVerdict> chunkVerdicts) {
        this(score, rationale, model, statements, chunkVerdicts, Optional.empty(), Optional.empty());
    }

    /**
     * Factory for verdicts without statement or chunk decomposition.
     *
     * @param score     the normalized quality score
     * @param rationale the judge's explanation
     * @param model     the model identifier
     * @return a Verdict with empty statements and chunkVerdicts lists
     */
    public static Verdict of(Score score, String rationale, String model) {
        return new Verdict(score, rationale, model, List.of(), List.of());
    }

    /**
     * Returns a copy of this verdict carrying the judge exchange: the exact prompt
     * sent to the LLM and the raw response it returned.
     *
     * @param prompt   the exact prompt sent to the judge LLM
     * @param response the raw LLM response
     * @return a new Verdict with {@code promptUsed} and {@code rawResponse} present
     */
    public Verdict withExchange(String prompt, String response) {
        return new Verdict(score, rationale, model, statements, chunkVerdicts,
                Optional.of(Objects.requireNonNull(prompt, "prompt")),
                Optional.of(Objects.requireNonNull(response, "response")));
    }

    /**
     * Computes the faithfulness score mechanically from the statements list.
     *
     * @return {@code supported / total} if statements is non-empty, empty otherwise
     */
    public OptionalDouble computedScore() {
        if (statements.isEmpty()) {
            return OptionalDouble.empty();
        }
        long supported = statements.stream().filter(Statement::supported).count();
        return OptionalDouble.of((double) supported / statements.size());
    }

    /**
     * Computes the RAGAS Average Precision score from per-chunk verdicts.
     *
     * <p>Formula: {@code AP = Σ (Precision@k × relevant_k) / total_relevant}
     * where the sum is over positions where {@code relevant_k = 1}.
     * This rewards pipelines that rank relevant chunks at the top of the list.
     *
     * @return the Average Precision in [0.0, 1.0] if chunkVerdicts is non-empty, empty otherwise
     */
    public OptionalDouble computedPrecision() {
        if (chunkVerdicts.isEmpty()) {
            return OptionalDouble.empty();
        }
        List<ChunkVerdict> sorted = chunkVerdicts.stream()
                .sorted((a, b) -> Integer.compare(a.rank(), b.rank()))
                .toList();
        long totalRelevant = sorted.stream().filter(ChunkVerdict::relevant).count();
        if (totalRelevant == 0) {
            return OptionalDouble.of(0.0);
        }
        double sum = 0.0;
        int relevantSoFar = 0;
        for (int i = 0; i < sorted.size(); i++) {
            ChunkVerdict cv = sorted.get(i);
            if (cv.relevant()) {
                relevantSoFar++;
                double precisionAtK = (double) relevantSoFar / (i + 1);
                sum += precisionAtK;
            }
        }
        return OptionalDouble.of(sum / totalRelevant);
    }

    /**
     * Returns true if this verdict's score meets or exceeds the given threshold.
     *
     * @param threshold the minimum acceptable score value in [0.0, 1.0]
     * @return true when {@code score.value() >= threshold}
     */
    public boolean isAboveThreshold(double threshold) {
        return score.value() >= threshold;
    }
}
