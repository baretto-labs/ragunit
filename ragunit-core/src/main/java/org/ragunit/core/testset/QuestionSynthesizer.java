package org.ragunit.core.testset;

import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.TestCase;

import java.util.List;

/**
 * Strategy for generating {@link TestCase} instances from a document corpus.
 *
 * <p>Implementations define how questions are synthesized:
 * <ul>
 *   <li>{@link SimpleQuestionSynthesizer} — one question per single document</li>
 *   <li>{@link MultiHopQuestionSynthesizer} — one question requiring multiple documents</li>
 * </ul>
 */
public interface QuestionSynthesizer {

    /**
     * Synthesizes up to {@code count} test cases from the given corpus.
     *
     * @param corpus the source documents
     * @param count  the number of test cases to generate
     * @return a list of generated test cases
     */
    List<TestCase> synthesize(List<Document> corpus, int count);
}
