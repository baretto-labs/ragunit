package org.ragunit.core.domain;

/**
 * The strategy used to generate a {@link TestCase} question.
 *
 * <ul>
 *   <li>{@link #SIMPLE} — the question can be answered from a single document.</li>
 *   <li>{@link #MULTI_HOP} — the question requires information from two or more documents.</li>
 * </ul>
 */
public enum QuestionType {
    /** Question answerable from a single retrieved document. */
    SIMPLE,
    /** Question requiring information from two or more documents. */
    MULTI_HOP
}
