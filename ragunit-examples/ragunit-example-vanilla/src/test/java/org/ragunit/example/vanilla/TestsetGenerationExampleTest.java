package org.ragunit.example.vanilla;

import org.junit.jupiter.api.Test;
import org.ragunit.core.RagTest;
import org.ragunit.core.assertion.RagAssert;
import org.ragunit.core.domain.Answer;
import org.ragunit.core.domain.TestCase;
import org.ragunit.core.domain.Testset;
import org.ragunit.core.judge.OllamaJudge;
import org.ragunit.core.judge.RagJudge;
import org.ragunit.core.testset.OllamaTestsetGenerator;
import org.ragunit.core.testset.TestsetGenerator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates testset generation and batch evaluation with plain Java + Ollama.
 *
 * <p>Generates synthetic test cases from a cloud API documentation corpus and
 * evaluates each generated case for faithfulness — simulating a pipeline that
 * returns the reference answer directly.
 *
 * <p>Prerequisites: Ollama running on localhost:11434 with {@code qwen2.5:14b} pulled.
 *
 * <p>Run: {@code mvn -pl ragunit-examples/ragunit-example-vanilla -Dgroups=rag-eval test}
 */
class TestsetGenerationExampleTest {

    private final TestsetGenerator generator = new OllamaTestsetGenerator("qwen2.5:14b");
    private final RagJudge judge = new OllamaJudge("qwen2.5:14b");

    @Test
    @RagTest
    void should_generateTestset_and_evaluate_each_case() {
        Testset testset = generator.generate(ApiDocCorpus.ALL, 3);

        assertThat(testset.isEmpty()).isFalse();

        for (TestCase tc : testset.cases()) {
            RagAssert.assertThatAnswer(simulatePipeline(tc))
                    .givenContext(tc.context())
                    .forQuestion(tc.question())
                    .evaluatedBy(judge)
                    .isFaithfulToContext(0.70);
        }
    }

    /**
     * Simulates a RAG pipeline by returning the reference answer directly.
     * In a real scenario this would call your retriever + LLM generator.
     */
    private static Answer simulatePipeline(TestCase tc) {
        return new Answer(tc.referenceAnswer().text());
    }
}
