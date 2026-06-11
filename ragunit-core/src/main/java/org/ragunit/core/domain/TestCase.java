package org.ragunit.core.domain;

import java.util.List;
import java.util.Objects;

/**
 * A single evaluation sample: a {@link Question}, the {@link Document} chunks that form
 * its relevant context, the {@link ReferenceAnswer} ground truth, and the
 * {@link QuestionType} that describes how the question was synthesized.
 *
 * <p>Use the static factories to create instances:
 * <pre>{@code
 * TestCase.simple(question, context, reference)
 * TestCase.multiHop(question, context, reference)
 * }</pre>
 *
 * @param question       the evaluation question
 * @param context        the retrieved document chunks forming the relevant context
 * @param referenceAnswer the ground-truth reference answer
 * @param questionType   how this question was synthesized
 */
public record TestCase(
        Question question,
        List<Document> context,
        ReferenceAnswer referenceAnswer,
        QuestionType questionType
) {
    /** Validates all fields are non-null and makes context unmodifiable. */
    public TestCase {
        Objects.requireNonNull(question, "question");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(referenceAnswer, "referenceAnswer");
        Objects.requireNonNull(questionType, "questionType");
        context = List.copyOf(context);
    }

    /**
     * Creates a {@link QuestionType#SIMPLE} test case.
     *
     * @param question        the evaluation question
     * @param context         the retrieved document chunks
     * @param referenceAnswer the ground-truth reference answer
     * @return a new TestCase with type SIMPLE
     */
    public static TestCase simple(Question question, List<Document> context,
                                  ReferenceAnswer referenceAnswer) {
        return new TestCase(question, context, referenceAnswer, QuestionType.SIMPLE);
    }

    /**
     * Creates a {@link QuestionType#MULTI_HOP} test case.
     *
     * @param question        the evaluation question
     * @param context         the retrieved document chunks (two or more)
     * @param referenceAnswer the ground-truth reference answer
     * @return a new TestCase with type MULTI_HOP
     */
    public static TestCase multiHop(Question question, List<Document> context,
                                    ReferenceAnswer referenceAnswer) {
        return new TestCase(question, context, referenceAnswer, QuestionType.MULTI_HOP);
    }
}
