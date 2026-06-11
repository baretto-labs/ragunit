package org.ragunit.core.testset;

import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.Testset;

import java.util.List;

/**
 * Generates a synthetic {@link Testset} from a corpus of {@link Document} instances
 * by prompting an LLM to produce question/reference-answer pairs.
 *
 * <p>Usage:
 * <pre>{@code
 * TestsetGenerator generator = new OllamaTestsetGenerator("qwen2.5:14b");
 * Testset testset = generator.generate(documents, 10);
 *
 * testset.cases().forEach(tc ->
 *     RagAssert.assertThatContext(tc.context())
 *              .forQuestion(tc.question())
 *              .evaluatedBy(judge)
 *              .hasContextRecall(tc.referenceAnswer(), 0.80)
 * );
 * }</pre>
 *
 */
public interface TestsetGenerator {

    /**
     * Generates up to {@code count} test cases from the given corpus.
     *
     * <p>If {@code count} is 0, returns an empty {@link Testset}.
     * If {@code count} exceeds the corpus size, one test case is generated per document.
     *
     * @param corpus the source documents from which questions are synthesized
     * @param count  the desired number of test cases
     * @return a {@link Testset} containing the generated test cases
     */
    Testset generate(List<Document> corpus, int count);
}
