package org.ragunit.core.judge;

import org.junit.jupiter.api.Test;
import org.ragunit.core.domain.Answer;
import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.PromptContext;
import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.ReferenceAnswer;
import org.ragunit.core.domain.ToolCall;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JudgePromptLibrary}: every built-in metric prompt is public,
 * versioned, and renders the evaluation inputs it needs.
 */
class JudgePromptLibraryTest {

    private static final Question QUESTION = new Question("What is the capital of France?");
    private static final List<Document> CONTEXT = List.of(new Document("Paris is the capital of France."));
    private static final Answer ANSWER = new Answer("Paris.");
    private static final ReferenceAnswer REFERENCE = new ReferenceAnswer("The capital of France is Paris.");
    private static final List<ToolCall> TRAJECTORY =
            List.of(new ToolCall("web_search", "capital of France", "Paris"));

    // --- defaults() map ---

    @Test
    void should_provideTemplateForEveryMetricType_when_requestingDefaults() {
        Map<MetricType, JudgePromptTemplate> defaults = JudgePromptLibrary.defaults();

        assertThat(defaults.keySet()).containsExactlyInAnyOrder(MetricType.values());
    }

    // --- rendering: each prompt carries its inputs ---

    @Test
    void should_renderQuestionAndContext_when_usingRetrievalV1() {
        String prompt = JudgePromptLibrary.RETRIEVAL_V1
                .render(PromptContext.forRetrieval(QUESTION, CONTEXT));

        assertThat(prompt)
                .contains(QUESTION.text())
                .contains(CONTEXT.get(0).content());
    }

    @Test
    void should_renderAnswerAndContext_when_usingFaithfulnessV1() {
        String prompt = JudgePromptLibrary.FAITHFULNESS_V1
                .render(PromptContext.forGeneration(QUESTION, CONTEXT, ANSWER));

        assertThat(prompt)
                .contains(ANSWER.text())
                .contains(CONTEXT.get(0).content());
    }

    @Test
    void should_renderQuestionAndAnswer_when_usingAnswerRelevancyV1() {
        String prompt = JudgePromptLibrary.ANSWER_RELEVANCY_V1
                .render(PromptContext.forGeneration(QUESTION, List.of(), ANSWER));

        assertThat(prompt)
                .contains(QUESTION.text())
                .contains(ANSWER.text());
    }

    @Test
    void should_renderChunksWithRanks_when_usingContextPrecisionV1() {
        String prompt = JudgePromptLibrary.CONTEXT_PRECISION_V1
                .render(PromptContext.forRetrieval(QUESTION, CONTEXT));

        assertThat(prompt)
                .contains("Rank 1")
                .contains(CONTEXT.get(0).content());
    }

    @Test
    void should_renderReferenceAnswer_when_usingContextRecallV1() {
        String prompt = JudgePromptLibrary.CONTEXT_RECALL_V1
                .render(PromptContext.forContextRecall(QUESTION, CONTEXT, REFERENCE));

        assertThat(prompt).contains(REFERENCE.text());
    }

    @Test
    void should_renderAnswerAndReference_when_usingFactualCorrectnessV1() {
        String prompt = JudgePromptLibrary.FACTUAL_CORRECTNESS_V1
                .render(PromptContext.forFactualCorrectness(QUESTION, ANSWER, REFERENCE));

        assertThat(prompt)
                .contains(ANSWER.text())
                .contains(REFERENCE.text());
    }

    @Test
    void should_renderToolCalls_when_usingToolTrajectoryV1() {
        String prompt = JudgePromptLibrary.TOOL_TRAJECTORY_V1
                .render(PromptContext.forToolTrajectory(QUESTION, TRAJECTORY, ANSWER));

        assertThat(prompt)
                .contains("web_search")
                .contains("capital of France");
    }

    @Test
    void should_renderRefusalAnswer_when_usingRejectionV1() {
        String prompt = JudgePromptLibrary.REJECTION_V1
                .render(PromptContext.forGeneration(QUESTION, CONTEXT, ANSWER));

        assertThat(prompt).contains(ANSWER.text());
    }

    @Test
    void should_renderQuestionAndContext_when_usingContextRejectionV1() {
        String prompt = JudgePromptLibrary.CONTEXT_REJECTION_V1
                .render(PromptContext.forRetrieval(QUESTION, CONTEXT));

        assertThat(prompt)
                .contains(QUESTION.text())
                .contains(CONTEXT.get(0).content());
    }

    @Test
    void should_renderAnswer_when_usingPromptInjectionV1() {
        String prompt = JudgePromptLibrary.PROMPT_INJECTION_V1
                .render(PromptContext.forGeneration(QUESTION, CONTEXT, ANSWER));

        assertThat(prompt).contains(ANSWER.text());
    }

    @Test
    void should_renderContext_when_usingContextPromptInjectionV1() {
        String prompt = JudgePromptLibrary.CONTEXT_PROMPT_INJECTION_V1
                .render(PromptContext.forRetrieval(QUESTION, CONTEXT));

        assertThat(prompt).contains(CONTEXT.get(0).content());
    }

    @Test
    void should_renderAnswer_when_usingPIILeakV1() {
        String prompt = JudgePromptLibrary.PII_LEAK_V1
                .render(PromptContext.forGeneration(QUESTION, CONTEXT, ANSWER));

        assertThat(prompt).contains(ANSWER.text());
    }

    @Test
    void should_renderContext_when_usingContextPIILeakV1() {
        String prompt = JudgePromptLibrary.CONTEXT_PII_LEAK_V1
                .render(PromptContext.forRetrieval(QUESTION, CONTEXT));

        assertThat(prompt).contains(CONTEXT.get(0).content());
    }

    // --- every default prompt enforces the JSON reply contract ---

    @Test
    void should_includeJsonReplyInstruction_when_renderingEveryDefaultPrompt() {
        PromptContext ctx = new PromptContext(QUESTION, CONTEXT,
                java.util.Optional.of(ANSWER), java.util.Optional.of(REFERENCE), TRAJECTORY);

        for (Map.Entry<MetricType, JudgePromptTemplate> entry : JudgePromptLibrary.defaults().entrySet()) {
            String prompt = entry.getValue().render(ctx);
            assertThat(prompt)
                    .as("prompt for %s must request a JSON reply", entry.getKey())
                    .contains("JSON")
                    .contains("score");
        }
    }
}
