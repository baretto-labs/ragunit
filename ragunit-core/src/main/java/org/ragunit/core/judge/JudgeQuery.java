package org.ragunit.core.judge;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A generic evaluation request: a {@link Criterion} plus arbitrary named inputs.
 *
 * <p>This is the non-RAG-shaped entry point to a {@link Judge}. Inputs are free-form
 * named text values (single or multi-valued) rendered into the judge prompt in
 * insertion order:
 * <pre>{@code
 * JudgeQuery query = JudgeQuery.builder()
 *         .criterion(Criterion.of("conciseness", "Is the summary concise and faithful?"))
 *         .input("Source", article)
 *         .input("Summary", summary)
 *         .build();
 * }</pre>
 *
 * <p>The canonical RAG input names ({@link #INPUT_QUESTION}, {@link #INPUT_CONTEXT},
 * {@link #INPUT_ANSWER}, {@link #INPUT_REFERENCE}) let {@link RagJudge} dispatch
 * queries whose criterion is a built-in {@link MetricType} to its typed methods.
 */
public final class JudgeQuery {

    /** Canonical input name for the user's question (RAG dispatch). */
    public static final String INPUT_QUESTION = "question";

    /** Canonical input name for the retrieved context, multi-valued (RAG dispatch). */
    public static final String INPUT_CONTEXT = "context";

    /** Canonical input name for the generated answer (RAG dispatch). */
    public static final String INPUT_ANSWER = "answer";

    /** Canonical input name for the ground-truth reference answer (RAG dispatch). */
    public static final String INPUT_REFERENCE = "reference";

    private final Criterion criterion;
    private final Map<String, List<String>> inputs;

    private JudgeQuery(Criterion criterion, Map<String, List<String>> inputs) {
        this.criterion = criterion;
        this.inputs = Collections.unmodifiableMap(new LinkedHashMap<>(inputs));
    }

    /**
     * Starts building a query.
     *
     * @return a new empty builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the criterion this query asks the judge to rate.
     *
     * @return the criterion, never null
     */
    public Criterion criterion() {
        return criterion;
    }

    /**
     * Returns all named inputs in insertion order.
     *
     * @return an unmodifiable ordered map of input name to values
     */
    public Map<String, List<String>> inputs() {
        return inputs;
    }

    /**
     * Returns the values of a named input, or an empty list if absent.
     *
     * @param name the input name
     * @return the values, never null
     */
    public List<String> inputValues(String name) {
        return inputs.getOrDefault(name, List.of());
    }

    /**
     * Returns the first value of a named input, if present.
     *
     * @param name the input name
     * @return the first value, or empty if the input is absent
     */
    public Optional<String> firstInput(String name) {
        return inputValues(name).stream().findFirst();
    }

    /**
     * Returns the first value of a named input, failing loudly when absent.
     *
     * @param name the input name
     * @return the first value
     * @throws JudgeException if the input is absent
     */
    public String requiredInput(String name) {
        return firstInput(name).orElseThrow(() -> new JudgeException(
                "JudgeQuery for criterion '%s' is missing required input '%s'"
                        .formatted(criterion.name(), name)));
    }

    /**
     * Renders the prompt body for this query: the criterion instruction followed
     * by every input in insertion order. Multi-valued inputs render as bullet lists.
     *
     * @return the deterministic prompt body sent to the judge
     */
    public String render() {
        String renderedInputs = inputs.entrySet().stream()
                .map(entry -> renderInput(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("\n\n"));
        return """
                Criterion: %s

                %s""".formatted(criterion.instruction(), renderedInputs);
    }

    private static String renderInput(String name, List<String> values) {
        if (values.size() == 1) {
            return name + ": " + values.get(0);
        }
        return name + ":\n" + values.stream()
                .map(value -> "- " + value)
                .collect(Collectors.joining("\n"));
    }

    /** Fluent builder for {@link JudgeQuery}. */
    public static final class Builder {

        private Criterion criterion;
        private final Map<String, List<String>> inputs = new LinkedHashMap<>();

        private Builder() {
        }

        /**
         * Sets the criterion to judge (required).
         *
         * @param evaluationCriterion the evaluation criterion
         * @return this, for chaining
         */
        public Builder criterion(Criterion evaluationCriterion) {
            this.criterion = Objects.requireNonNull(evaluationCriterion, "criterion");
            return this;
        }

        /**
         * Adds a single-valued named input.
         *
         * @param name  the input name, e.g. {@code "Summary"}
         * @param value the input text
         * @return this, for chaining
         */
        public Builder input(String name, String value) {
            return input(name, List.of(Objects.requireNonNull(value, "value")));
        }

        /**
         * Adds a multi-valued named input (rendered as a bullet list).
         *
         * @param name   the input name, e.g. {@code "context"}
         * @param values the input values
         * @return this, for chaining
         */
        public Builder input(String name, List<String> values) {
            inputs.put(Objects.requireNonNull(name, "name"),
                    List.copyOf(Objects.requireNonNull(values, "values")));
            return this;
        }

        /**
         * Builds the query.
         *
         * @return an immutable JudgeQuery
         * @throws IllegalStateException if no criterion or no input was set
         */
        public JudgeQuery build() {
            if (criterion == null) {
                throw new IllegalStateException("JudgeQuery requires a criterion");
            }
            if (inputs.isEmpty()) {
                throw new IllegalStateException("JudgeQuery requires at least one input");
            }
            return new JudgeQuery(criterion, inputs);
        }
    }
}
