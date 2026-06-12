package org.ragunit.core.judge;

import java.util.Objects;

/**
 * The named evaluation question asked to a {@link Judge}: what quality should
 * the judge rate, expressed as a human-readable instruction.
 *
 * <p>Built-in RAG metrics implement this interface via {@link MetricType}.
 * For any other quality, create an ad-hoc criterion:
 * <pre>{@code
 * Criterion conciseness = Criterion.of("conciseness",
 *         "Is the summary concise while staying faithful to the source?");
 * }</pre>
 *
 * <p>User-defined enums can also implement this interface to catalogue
 * project-specific criteria.
 */
public interface Criterion {

    /**
     * The short identifier of this criterion, e.g. {@code "conciseness"}.
     *
     * @return the criterion name, never blank
     */
    String name();

    /**
     * The evaluation question the judge must answer, e.g.
     * {@code "Is the summary concise while staying faithful to the source?"}.
     *
     * @return the instruction text, never blank
     */
    String instruction();

    /**
     * Creates an ad-hoc criterion from a name and an instruction.
     *
     * @param name        the short identifier; must not be blank
     * @param instruction the evaluation question; must not be blank
     * @return an immutable criterion
     * @throws IllegalArgumentException if name or instruction is blank
     */
    static Criterion of(String name, String instruction) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(instruction, "instruction");
        if (name.isBlank() || instruction.isBlank()) {
            throw new IllegalArgumentException("Criterion name and instruction must not be blank");
        }
        /** Immutable ad-hoc criterion carrying a name and an instruction. */
        record NamedCriterion(String name, String instruction) implements Criterion {
        }
        return new NamedCriterion(name, instruction);
    }
}
