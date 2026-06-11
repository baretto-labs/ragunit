package org.ragunit.core.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Carries the evaluation inputs that a {@link org.ragunit.core.judge.JudgePromptTemplate}
 * can use to render a custom prompt.
 *
 * <p>Only the inputs relevant to the evaluated metric are present:
 * {@code answer} is empty for retrieval-only metrics, {@code reference} is empty
 * unless the metric compares against a ground truth (CONTEXT_RECALL,
 * FACTUAL_CORRECTNESS), and {@code trajectory} is empty unless the metric
 * evaluates agentic tool calls (TOOL_TRAJECTORY).
 *
 * @param question         the user's original query
 * @param retrievedContext the list of documents retrieved by the Retriever
 * @param answer           the generated answer, or empty if not applicable
 * @param reference        the ground-truth reference answer, or empty if not applicable
 * @param trajectory       the ordered tool calls of an agentic pipeline (may be empty)
 */
public record PromptContext(Question question, List<Document> retrievedContext,
                            Optional<Answer> answer, Optional<ReferenceAnswer> reference,
                            List<ToolCall> trajectory) {

    /** Validates all fields and makes both lists unmodifiable. */
    public PromptContext {
        Objects.requireNonNull(question, "question");
        Objects.requireNonNull(retrievedContext, "retrievedContext");
        Objects.requireNonNull(answer, "answer");
        Objects.requireNonNull(reference, "reference");
        Objects.requireNonNull(trajectory, "trajectory");
        retrievedContext = List.copyOf(retrievedContext);
        trajectory = List.copyOf(trajectory);
    }

    /**
     * Creates a PromptContext for retrieval-only metrics (no answer).
     *
     * @param question the user's original query
     * @param context  the list of retrieved documents
     * @return a PromptContext with an empty answer
     */
    public static PromptContext forRetrieval(Question question, List<Document> context) {
        return new PromptContext(question, context, Optional.empty(), Optional.empty(), List.of());
    }

    /**
     * Creates a PromptContext for generation metrics (answer is present).
     *
     * @param question the user's original query
     * @param context  the list of retrieved documents
     * @param answer   the generated answer
     * @return a PromptContext with the answer present
     */
    public static PromptContext forGeneration(Question question, List<Document> context, Answer answer) {
        return new PromptContext(question, context,
                Optional.of(Objects.requireNonNull(answer, "answer")), Optional.empty(), List.of());
    }

    /**
     * Creates a PromptContext for ContextRecall evaluation (reference is present).
     *
     * @param question  the user's original query
     * @param context   the list of retrieved documents
     * @param reference the ground-truth reference answer
     * @return a PromptContext with the reference present and an empty answer
     */
    public static PromptContext forContextRecall(Question question, List<Document> context,
                                                 ReferenceAnswer reference) {
        return new PromptContext(question, context, Optional.empty(),
                Optional.of(Objects.requireNonNull(reference, "reference")), List.of());
    }

    /**
     * Creates a PromptContext for FactualCorrectness evaluation
     * (answer and reference are present, no retrieved context).
     *
     * @param question  the user's original query
     * @param answer    the generated answer
     * @param reference the ground-truth reference answer
     * @return a PromptContext with answer and reference present
     */
    public static PromptContext forFactualCorrectness(Question question, Answer answer,
                                                      ReferenceAnswer reference) {
        return new PromptContext(question, List.of(),
                Optional.of(Objects.requireNonNull(answer, "answer")),
                Optional.of(Objects.requireNonNull(reference, "reference")), List.of());
    }

    /**
     * Creates a PromptContext for ToolTrajectory evaluation
     * (trajectory and answer are present, no retrieved context).
     *
     * @param question   the user's original query
     * @param trajectory the ordered list of tool calls
     * @param answer     the final generated answer
     * @return a PromptContext with trajectory and answer present
     */
    public static PromptContext forToolTrajectory(Question question, List<ToolCall> trajectory,
                                                  Answer answer) {
        return new PromptContext(question, List.of(),
                Optional.of(Objects.requireNonNull(answer, "answer")), Optional.empty(),
                Objects.requireNonNull(trajectory, "trajectory"));
    }
}
