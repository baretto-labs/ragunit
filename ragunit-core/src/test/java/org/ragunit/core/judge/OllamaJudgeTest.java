package org.ragunit.core.judge;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ragunit.core.domain.Answer;
import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.PromptContext;
import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.ReferenceAnswer;
import org.ragunit.core.domain.ToolCall;
import org.ragunit.core.domain.Verdict;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for OllamaJudge using a JDK-native HTTP server to mock the Ollama API.
 *
 * <p>No external process is required — the mock server runs in-process.
 */
class OllamaJudgeTest {

    private static final String MODEL = "llama3:8b";
    private static final double SCORE_09 = 0.9;
    private static final double SCORE_075 = 0.75;
    private static final int HTTP_OK = 200;
    private static final int OLLAMA_DEFAULT_PORT = 11434;
    private static final Question QUESTION = new Question("What is the capital of France?");
    private static final List<Document> CONTEXT = List.of(new Document("Paris is the capital of France."));
    private static final Answer ANSWER = new Answer("Paris.");

    private HttpServer mockServer;
    private int port;

    @BeforeEach
    void startMockServer() throws IOException {
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        port = mockServer.getAddress().getPort();
    }

    @AfterEach
    void stopMockServer() {
        mockServer.stop(0);
    }

    private void registerHandler(String responseBody) {
        registerCapturingHandler(responseBody, new AtomicReference<>());
    }

    private void registerCapturingHandler(String responseBody, AtomicReference<String> bodyRef) {
        mockServer.createContext("/api/chat", exchange -> {
            try (InputStream in = exchange.getRequestBody()) {
                bodyRef.set(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(HTTP_OK, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        mockServer.start();
    }

    private OllamaJudge judge() {
        return new OllamaJudge(MODEL, "localhost", port);
    }

    /** Builds a mock Ollama API response wrapping the given JSON as the LLM content. */
    private static String ollamaJsonResponse(String json) {
        String escaped = json.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"message\":{\"role\":\"assistant\",\"content\":\"" + escaped + "\"}}";
    }

    // --- evaluateRetrieval ---

    @Test
    void should_returnVerdict_when_retrievalResponseContainsScoreAndRationale() {
        registerHandler(ollamaJsonResponse(
                "{\"score\": 0.9, \"rationale\": \"Highly relevant.\", \"statements\": []}"));

        Verdict verdict = judge().evaluateRetrieval(QUESTION, CONTEXT);

        assertThat(verdict.score().value()).isEqualTo(SCORE_09);
        assertThat(verdict.rationale()).isEqualTo("Highly relevant.");
        assertThat(verdict.model()).isEqualTo(MODEL);
    }

    @Test
    void should_returnEmptyStatements_when_retrievalResponseHasNoStatements() {
        registerHandler(ollamaJsonResponse(
                "{\"score\": 0.9, \"rationale\": \"Relevant.\", \"statements\": []}"));

        Verdict verdict = judge().evaluateRetrieval(QUESTION, CONTEXT);

        assertThat(verdict.statements()).isEmpty();
    }

    @Test
    void should_returnZeroScore_when_contentIsNotJson() {
        registerHandler(ollamaJsonResponse("I cannot determine a score."));

        Verdict verdict = judge().evaluateRetrieval(QUESTION, CONTEXT);

        assertThat(verdict.score().value()).isEqualTo(0.0);
    }

    @Test
    void should_throwJudgeException_when_serverIsUnreachableForRetrieval() {
        OllamaJudge unreachable = new OllamaJudge(MODEL, "localhost", 1);

        assertThatThrownBy(() -> unreachable.evaluateRetrieval(QUESTION, CONTEXT))
                .isInstanceOf(JudgeException.class);
    }

    // --- evaluateGeneration ---

    @Test
    void should_returnVerdict_when_generationResponseContainsScoreAndStatements() {
        registerHandler(ollamaJsonResponse(
                "{\"score\": 0.75, \"rationale\": \"Mostly faithful.\", \"statements\": ["
                + "{\"text\": \"Paris is the capital.\", \"supported\": true},"
                + "{\"text\": \"Paris has 10M people.\", \"supported\": false}]}"));

        Verdict verdict = judge().evaluateGeneration(QUESTION, CONTEXT, ANSWER);

        assertThat(verdict.score().value()).isEqualTo(SCORE_075);
        assertThat(verdict.rationale()).isEqualTo("Mostly faithful.");
        assertThat(verdict.statements()).hasSize(2);
        assertThat(verdict.statements().get(0).supported()).isTrue();
        assertThat(verdict.statements().get(1).supported()).isFalse();
    }

    @Test
    void should_returnZeroScore_when_generationContentIsNotJson() {
        registerHandler(ollamaJsonResponse("No score available."));

        Verdict verdict = judge().evaluateGeneration(QUESTION, CONTEXT, ANSWER);

        assertThat(verdict.score().value()).isEqualTo(0.0);
    }

    @Test
    void should_throwJudgeException_when_serverIsUnreachableForGeneration() {
        OllamaJudge unreachable = new OllamaJudge(MODEL, "localhost", 1);

        assertThatThrownBy(() -> unreachable.evaluateGeneration(QUESTION, CONTEXT, ANSWER))
                .isInstanceOf(JudgeException.class);
    }

    @Test
    void should_throwJudgeException_when_responseBodyIsInvalidJson() {
        registerHandler("not-json-at-all");

        assertThatThrownBy(() -> judge().evaluateRetrieval(QUESTION, CONTEXT))
                .isInstanceOf(JudgeException.class)
                .hasMessageContaining("parse");
    }

    @Test
    void should_useDefaultHostAndPort_when_constructedWithModelOnly() {
        assertThat(OllamaJudge.DEFAULT_HOST).isEqualTo("localhost");
        assertThat(OllamaJudge.DEFAULT_PORT).isEqualTo(OLLAMA_DEFAULT_PORT);
    }

    @Test
    void should_connectToCustomHostAndPort_when_specifiedAtConstruction() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        registerCapturingHandler(ollamaJsonResponse(
                "{\"score\": 0.6, \"rationale\": \"OK.\", \"statements\": []}"), capturedBody);

        new OllamaJudge(MODEL, "localhost", port).evaluateRetrieval(QUESTION, CONTEXT);

        assertThat(capturedBody.get()).isNotNull();
    }

    @Test
    void should_includeQuestionInRequestBody_when_evaluatingRetrieval() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        registerCapturingHandler(ollamaJsonResponse(
                "{\"score\": 0.8, \"rationale\": \"Good.\", \"statements\": []}"), capturedBody);

        judge().evaluateRetrieval(QUESTION, CONTEXT);

        assertThat(capturedBody.get()).contains(QUESTION.text());
        assertThat(capturedBody.get()).contains(MODEL);
    }

    @Test
    void should_includeAnswerInRequestBody_when_evaluatingGeneration() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        registerCapturingHandler(ollamaJsonResponse(
                "{\"score\": 0.8, \"rationale\": \"Good.\", \"statements\": []}"), capturedBody);

        judge().evaluateGeneration(QUESTION, CONTEXT, ANSWER);

        assertThat(capturedBody.get()).contains(ANSWER.text());
        assertThat(capturedBody.get()).contains(MODEL);
    }

    @Test
    void should_includeJsonFormatInstructions_when_evaluatingRetrieval() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        registerCapturingHandler(ollamaJsonResponse(
                "{\"score\": 0.8, \"rationale\": \"Good.\", \"statements\": []}"), capturedBody);

        judge().evaluateRetrieval(QUESTION, CONTEXT);

        assertThat(capturedBody.get()).contains("JSON");
        assertThat(capturedBody.get()).contains("score");
    }

    @Test
    void should_includeStatementsInstructionInPrompt_when_evaluatingGeneration() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        registerCapturingHandler(ollamaJsonResponse(
                "{\"score\": 0.8, \"rationale\": \"Good.\", \"statements\": []}"), capturedBody);

        judge().evaluateGeneration(QUESTION, CONTEXT, ANSWER);

        assertThat(capturedBody.get()).contains("statements");
        assertThat(capturedBody.get()).contains("supported");
    }

    @Test
    void should_escapeSpecialCharsInModel_when_modelContainsBackslash() {
        String trickyModel = "llama\\3:8b";
        AtomicReference<String> capturedBody = new AtomicReference<>();
        registerCapturingHandler(ollamaJsonResponse(
                "{\"score\": 0.5, \"rationale\": \"OK.\", \"statements\": []}"), capturedBody);

        new OllamaJudge(trickyModel, "localhost", port).evaluateRetrieval(QUESTION, CONTEXT);

        assertThat(capturedBody.get()).contains("llama\\\\3:8b");
    }

    @Test
    void should_parseContentCorrectly_when_roleFieldContainsScoreKeyword() {
        // Guards the extractContent +/- offset: if wrong, role value is parsed instead of content.
        String json = "{\"score\": 0.9, \"rationale\": \"Correct.\", \"statements\": []}";
        String escaped = json.replace("\\", "\\\\").replace("\"", "\\\"");
        String response = "{\"message\":{\"role\":\"score: 0.5 judge\",\"content\":\"" + escaped + "\"}}";
        registerHandler(response);

        Verdict verdict = judge().evaluateRetrieval(QUESTION, CONTEXT);

        assertThat(verdict.score().value()).isEqualTo(SCORE_09);
    }

    // --- evaluateContextRecall ---

    private static final ReferenceAnswer REFERENCE = new ReferenceAnswer("Paris is the capital of France.");

    @Test
    void should_returnReferenceClaimsAsStatements_when_judgeResponseIsJson() {
        String json = "{\"score\": 0.75, \"rationale\": \"3 of 4 claims covered.\", \"statements\": ["
                + "{\"text\": \"Paris is the capital.\", \"supported\": true},"
                + "{\"text\": \"France is in Europe.\", \"supported\": false}]}";
        registerHandler(ollamaJsonResponse(json));

        Verdict verdict = judge().evaluateContextRecall(QUESTION, CONTEXT, REFERENCE);

        assertThat(verdict.statements()).hasSize(2);
        assertThat(verdict.statements().get(0).supported()).isTrue();
        assertThat(verdict.statements().get(1).supported()).isFalse();
    }

    @Test
    void should_includeReferenceAnswerInPrompt_when_evaluatingContextRecall() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        String json = "{\"score\": 1.0, \"rationale\": \"All covered.\", \"statements\": []}";
        registerCapturingHandler(ollamaJsonResponse(json), capturedBody);

        judge().evaluateContextRecall(QUESTION, CONTEXT, REFERENCE);

        assertThat(capturedBody.get()).contains("Paris is the capital of France.");
        assertThat(capturedBody.get()).contains("Reference Answer");
    }

    // --- evaluateAnswerRelevancy ---

    @Test
    void should_returnHypotheticalQuestionsAsStatements_when_judgeResponseIsJson() {
        String json = "{\"score\": 0.9, \"rationale\": \"Aligned.\", \"statements\": ["
                + "{\"text\": \"What is the capital of France?\", \"supported\": true},"
                + "{\"text\": \"Which city is France's capital?\", \"supported\": true},"
                + "{\"text\": \"What is the largest city in Europe?\", \"supported\": false}]}";
        registerHandler(ollamaJsonResponse(json));

        Verdict verdict = judge().evaluateAnswerRelevancy(QUESTION, ANSWER);

        assertThat(verdict.statements()).hasSize(3);
        assertThat(verdict.statements().get(0).supported()).isTrue();
        assertThat(verdict.statements().get(2).supported()).isFalse();
    }

    @Test
    void should_includeQuestionAndAnswerInPrompt_when_evaluatingAnswerRelevancy() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        String json = "{\"score\": 0.9, \"rationale\": \"OK.\", \"statements\": []}";
        registerCapturingHandler(ollamaJsonResponse(json), capturedBody);

        judge().evaluateAnswerRelevancy(QUESTION, ANSWER);

        assertThat(capturedBody.get()).contains("What is the capital of France?");
        assertThat(capturedBody.get()).contains("Paris.");
    }

    // --- evaluateContextPrecision ---

    @Test
    void should_returnChunkVerdictsInVerdict_when_judgeResponseIsJson() {
        String json = "{\"score\": 0.87, \"rationale\": \"Rank 1 relevant, rank 2 not.\","
                + " \"statements\": [],"
                + " \"chunks\": [{\"rank\": 1, \"relevant\": true}]}";
        registerHandler(ollamaJsonResponse(json));

        Verdict verdict = judge().evaluateContextPrecision(QUESTION, CONTEXT);

        assertThat(verdict.chunkVerdicts()).hasSize(1);
        assertThat(verdict.chunkVerdicts().get(0).relevant()).isTrue();
        assertThat(verdict.chunkVerdicts().get(0).chunk()).isEqualTo(CONTEXT.get(0));
    }

    @Test
    void should_includeAllChunksInPrompt_when_evaluatingContextPrecision() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        String json = "{\"score\": 1.0, \"rationale\": \"All relevant.\", \"statements\": [], \"chunks\": [{\"rank\": 1, \"relevant\": true}]}";
        registerCapturingHandler(ollamaJsonResponse(json), capturedBody);

        judge().evaluateContextPrecision(QUESTION, CONTEXT);

        assertThat(capturedBody.get()).contains("Rank 1");
        assertThat(capturedBody.get()).contains("Paris is the capital of France.");
    }

    // --- JudgePromptTemplate ---

    @Test
    void should_useCustomTemplate_when_templateIsConfiguredForMetricType() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        String json = "{\"score\": 0.9, \"rationale\": \"OK.\", \"statements\": []}";
        registerCapturingHandler(ollamaJsonResponse(json), capturedBody);

        OllamaJudge customJudge = new OllamaJudge(MODEL, "localhost", port,
                Map.of(MetricType.RETRIEVAL, ctx -> "CUSTOM_RETRIEVAL_PROMPT for " + ctx.question().text()));

        customJudge.evaluateRetrieval(QUESTION, CONTEXT);

        assertThat(capturedBody.get()).contains("CUSTOM_RETRIEVAL_PROMPT for");
        assertThat(capturedBody.get()).contains("What is the capital of France?");
    }

    @Test
    void should_useDefaultTemplate_when_noTemplateConfiguredForMetricType() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        String json = "{\"score\": 0.9, \"rationale\": \"OK.\", \"statements\": []}";
        registerCapturingHandler(ollamaJsonResponse(json), capturedBody);

        OllamaJudge customJudge = new OllamaJudge(MODEL, "localhost", port,
                Map.of(MetricType.GENERATION, ctx -> "CUSTOM_GENERATION_PROMPT"));

        customJudge.evaluateRetrieval(QUESTION, CONTEXT);

        assertThat(capturedBody.get()).doesNotContain("CUSTOM_GENERATION_PROMPT");
        assertThat(capturedBody.get()).contains("Rate how relevant");
    }

    @Test
    void should_callCustomTemplate_with_correctPromptContext() {
        AtomicReference<PromptContext> capturedCtx = new AtomicReference<>();
        String json = "{\"score\": 0.9, \"rationale\": \"OK.\", \"statements\": []}";
        registerHandler(ollamaJsonResponse(json));

        OllamaJudge customJudge = new OllamaJudge(MODEL, "localhost", port,
                Map.of(MetricType.RETRIEVAL, ctx -> {
                    capturedCtx.set(ctx);
                    return "CUSTOM";
                }));

        customJudge.evaluateRetrieval(QUESTION, CONTEXT);

        assertThat(capturedCtx.get()).isNotNull();
        assertThat(capturedCtx.get().question()).isEqualTo(QUESTION);
        assertThat(capturedCtx.get().retrievedContext()).isEqualTo(CONTEXT);
        assertThat(capturedCtx.get().answer()).isEmpty();
    }

    // --- evaluateToolTrajectory ---

    private static final ToolCall TOOL_CALL = new ToolCall("web_search", "capital of France", "Paris");
    private static final List<ToolCall> TRAJECTORY = List.of(TOOL_CALL);

    @Test
    void should_returnVerdict_when_trajectoryResponseContainsScoreAndRationale() {
        String json = "{\"score\": 0.9, \"rationale\": \"All tools necessary.\", \"statements\": []}";
        registerHandler(ollamaJsonResponse(json));

        Verdict verdict = judge().evaluateToolTrajectory(QUESTION, TRAJECTORY, ANSWER);

        assertThat(verdict.score().value()).isEqualTo(SCORE_09);
        assertThat(verdict.rationale()).isEqualTo("All tools necessary.");
    }

    @Test
    void should_includeToolCallsInPrompt_when_evaluatingTrajectory() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        String json = "{\"score\": 0.9, \"rationale\": \"ok\", \"statements\": []}";
        registerCapturingHandler(ollamaJsonResponse(json), capturedBody);

        judge().evaluateToolTrajectory(QUESTION, TRAJECTORY, ANSWER);

        assertThat(capturedBody.get()).contains("web_search");
        assertThat(capturedBody.get()).contains("capital of France");
        assertThat(capturedBody.get()).contains("Paris");
    }

    // --- evaluateContextRejection ---

    @Test
    void should_returnVerdict_when_evaluatingContextRejection() {
        registerHandler(ollamaJsonResponse(
                "{\"score\": 0.9, \"rationale\": \"Context is insufficient.\", \"statements\": []}"));

        Verdict verdict = judge().evaluateContextRejection(QUESTION, CONTEXT);

        assertThat(verdict.score().value()).isEqualTo(SCORE_09);
        assertThat(verdict.rationale()).isEqualTo("Context is insufficient.");
    }

    // --- evaluateRejection ---

    @Test
    void should_returnVerdict_when_evaluatingRejection() {
        registerHandler(ollamaJsonResponse(
                "{\"score\": 0.9, \"rationale\": \"Refusal was justified.\", \"statements\": []}"));

        Verdict verdict = judge().evaluateRejection(QUESTION, CONTEXT, ANSWER);

        assertThat(verdict.score().value()).isEqualTo(SCORE_09);
        assertThat(verdict.rationale()).isEqualTo("Refusal was justified.");
    }

    // --- evaluateContextPromptInjection ---

    @Test
    void should_returnVerdict_when_evaluatingContextPromptInjection() {
        registerHandler(ollamaJsonResponse(
                "{\"score\": 0.9, \"rationale\": \"No injection found.\", \"statements\": []}"));

        Verdict verdict = judge().evaluateContextPromptInjection(QUESTION, CONTEXT);

        assertThat(verdict.score().value()).isEqualTo(SCORE_09);
    }

    // --- evaluatePromptInjection ---

    @Test
    void should_returnVerdict_when_evaluatingPromptInjection() {
        registerHandler(ollamaJsonResponse(
                "{\"score\": 0.9, \"rationale\": \"Answer is clean.\", \"statements\": []}"));

        Verdict verdict = judge().evaluatePromptInjection(QUESTION, CONTEXT, ANSWER);

        assertThat(verdict.score().value()).isEqualTo(SCORE_09);
    }

    // --- evaluateContextPIILeak ---

    @Test
    void should_returnVerdict_when_evaluatingContextPIILeak() {
        registerHandler(ollamaJsonResponse(
                "{\"score\": 0.9, \"rationale\": \"No PII in context.\", \"statements\": []}"));

        Verdict verdict = judge().evaluateContextPIILeak(QUESTION, CONTEXT);

        assertThat(verdict.score().value()).isEqualTo(SCORE_09);
    }

    // --- evaluatePIILeak ---

    @Test
    void should_returnVerdict_when_evaluatingPIILeak() {
        registerHandler(ollamaJsonResponse(
                "{\"score\": 0.9, \"rationale\": \"No PII in answer.\", \"statements\": []}"));

        Verdict verdict = judge().evaluatePIILeak(QUESTION, CONTEXT, ANSWER);

        assertThat(verdict.score().value()).isEqualTo(SCORE_09);
    }

    // --- builder ---

    @Test
    void should_useCustomFaithfulnessPrompt_when_configuredViaBuilder() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        String json = "{\"score\": 0.9, \"rationale\": \"OK.\", \"statements\": []}";
        registerCapturingHandler(ollamaJsonResponse(json), capturedBody);

        OllamaJudge customJudge = OllamaJudge.builder()
                .model(MODEL)
                .host("localhost")
                .port(port)
                .faithfulnessPrompt(ctx -> "CUSTOM_FAITHFULNESS_PROMPT for " + ctx.question().text())
                .build();

        customJudge.evaluateGeneration(QUESTION, CONTEXT, ANSWER);

        assertThat(capturedBody.get()).contains("CUSTOM_FAITHFULNESS_PROMPT for");
    }

    @Test
    void should_useDefaultPrompts_when_builderHasNoCustomTemplate() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        String json = "{\"score\": 0.9, \"rationale\": \"OK.\", \"statements\": []}";
        registerCapturingHandler(ollamaJsonResponse(json), capturedBody);

        OllamaJudge defaultJudge = OllamaJudge.builder()
                .model(MODEL)
                .host("localhost")
                .port(port)
                .build();

        defaultJudge.evaluateRetrieval(QUESTION, CONTEXT);

        assertThat(capturedBody.get()).contains("Rate how relevant");
    }

    @Test
    void should_useCustomTemplateForAnyMetric_when_configuredViaGenericBuilderMethod() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        String json = "{\"score\": 0.9, \"rationale\": \"OK.\", \"statements\": []}";
        registerCapturingHandler(ollamaJsonResponse(json), capturedBody);

        OllamaJudge customJudge = OllamaJudge.builder()
                .model(MODEL)
                .host("localhost")
                .port(port)
                .prompt(MetricType.CONTEXT_RECALL, ctx -> "CUSTOM_RECALL_PROMPT")
                .build();

        customJudge.evaluateContextRecall(QUESTION, CONTEXT, REFERENCE);

        assertThat(capturedBody.get()).contains("CUSTOM_RECALL_PROMPT");
    }

    @Test
    void should_useCustomAnswerRelevancyPrompt_when_configuredViaBuilder() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        String json = "{\"score\": 0.9, \"rationale\": \"OK.\", \"statements\": []}";
        registerCapturingHandler(ollamaJsonResponse(json), capturedBody);

        OllamaJudge customJudge = OllamaJudge.builder()
                .model(MODEL)
                .host("localhost")
                .port(port)
                .answerRelevancyPrompt(ctx -> "CUSTOM_RELEVANCY_PROMPT")
                .build();

        customJudge.evaluateAnswerRelevancy(QUESTION, ANSWER);

        assertThat(capturedBody.get()).contains("CUSTOM_RELEVANCY_PROMPT");
    }

    @Test
    void should_useCustomFactualCorrectnessPrompt_when_configuredViaBuilder() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        String json = "{\"score\": 0.9, \"precision\": 0.9, \"recall\": 0.9,"
                + " \"rationale\": \"OK.\", \"statements\": []}";
        registerCapturingHandler(ollamaJsonResponse(json), capturedBody);

        OllamaJudge customJudge = OllamaJudge.builder()
                .model(MODEL)
                .host("localhost")
                .port(port)
                .factualCorrectnessPrompt(ctx -> "CUSTOM_FACTUAL_PROMPT")
                .build();

        assertThat(customJudge.evaluateFactualCorrectness(QUESTION, ANSWER, REFERENCE)).isNotNull();
        assertThat(capturedBody.get()).contains("CUSTOM_FACTUAL_PROMPT");
    }

    @Test
    void should_throwNullPointerException_when_builderHasNoModel() {
        assertThatThrownBy(() -> OllamaJudge.builder().build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("model");
    }

    // --- generic JudgeQuery evaluation ---

    @Test
    void should_sendCriterionInstructionAndInputs_when_evaluatingCustomCriterionQuery() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        String json = "{\"score\": 0.8, \"rationale\": \"Concise.\", \"statements\": []}";
        registerCapturingHandler(ollamaJsonResponse(json), capturedBody);

        JudgeQuery query = JudgeQuery.builder()
                .criterion(Criterion.of("conciseness", "Is the summary concise and faithful?"))
                .input("Source", "A long article about France.")
                .input("Summary", "France, briefly.")
                .build();

        JudgeResult result = judge().evaluate(query);

        assertThat(capturedBody.get()).contains("Is the summary concise and faithful?");
        assertThat(capturedBody.get()).contains("France, briefly.");
        assertThat(result.score()).isEqualTo(0.8);
        assertThat(result.justification()).isEqualTo("Concise.");
    }

    @Test
    void should_exposePromptAndRawResponse_when_evaluatingCustomCriterionQuery() {
        String json = "{\"score\": 0.8, \"rationale\": \"OK.\", \"statements\": []}";
        registerHandler(ollamaJsonResponse(json));

        JudgeQuery query = JudgeQuery.builder()
                .criterion(Criterion.of("clarity", "Is the text clear?"))
                .input("Text", "Some text.")
                .build();

        JudgeResult result = judge().evaluate(query);

        assertThat(result.promptUsed()).contains("Is the text clear?");
        assertThat(result.rawResponse()).contains("\"score\": 0.8");
    }

    @Test
    void should_useMetricPromptTemplate_when_queryCriterionIsBuiltInMetric() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        String json = "{\"score\": 0.9, \"rationale\": \"OK.\", \"statements\": []}";
        registerCapturingHandler(ollamaJsonResponse(json), capturedBody);

        JudgeQuery query = JudgeQuery.builder()
                .criterion(MetricType.RETRIEVAL)
                .input(JudgeQuery.INPUT_QUESTION, QUESTION.text())
                .input(JudgeQuery.INPUT_CONTEXT, List.of("Paris is the capital."))
                .build();

        judge().evaluate(query);

        assertThat(capturedBody.get()).contains("Rate how relevant");
    }

    // --- prompt and raw response exposure ---

    @Test
    void should_attachPromptUsedToVerdict_when_evaluatingRetrieval() {
        registerHandler(ollamaJsonResponse(
                "{\"score\": 0.9, \"rationale\": \"OK.\", \"statements\": []}"));

        Verdict verdict = judge().evaluateRetrieval(QUESTION, CONTEXT);

        assertThat(verdict.promptUsed()).hasValueSatisfying(prompt ->
                assertThat(prompt).contains(QUESTION.text()));
    }

    @Test
    void should_attachRawResponseToVerdict_when_evaluatingRetrieval() {
        registerHandler(ollamaJsonResponse(
                "{\"score\": 0.9, \"rationale\": \"OK.\", \"statements\": []}"));

        Verdict verdict = judge().evaluateRetrieval(QUESTION, CONTEXT);

        assertThat(verdict.rawResponse()).hasValueSatisfying(raw ->
                assertThat(raw).contains("\"score\": 0.9"));
    }

    @Test
    void should_attachExchangeToVerdict_when_evaluatingContextPrecision() {
        registerHandler(ollamaJsonResponse(
                "{\"score\": 1.0, \"rationale\": \"OK.\", \"statements\": [],"
                + " \"chunks\": [{\"rank\": 1, \"relevant\": true}]}"));

        Verdict verdict = judge().evaluateContextPrecision(QUESTION, CONTEXT);

        assertThat(verdict.promptUsed()).isPresent();
        assertThat(verdict.rawResponse()).isPresent();
    }

    // --- temperature ---

    @Test
    void should_sendTemperatureZero_when_notConfigured() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        String json = "{\"score\": 0.9, \"rationale\": \"OK.\", \"statements\": []}";
        registerCapturingHandler(ollamaJsonResponse(json), capturedBody);

        judge().evaluateRetrieval(QUESTION, CONTEXT);

        assertThat(capturedBody.get()).contains("\"temperature\":0.0");
    }

    @Test
    void should_sendCustomTemperature_when_configuredViaBuilder() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        String json = "{\"score\": 0.9, \"rationale\": \"OK.\", \"statements\": []}";
        registerCapturingHandler(ollamaJsonResponse(json), capturedBody);

        OllamaJudge warmJudge = OllamaJudge.builder()
                .model(MODEL)
                .host("localhost")
                .port(port)
                .temperature(0.7)
                .build();

        warmJudge.evaluateRetrieval(QUESTION, CONTEXT);

        assertThat(capturedBody.get()).contains("\"temperature\":0.7");
    }

    // --- custom templates honored for all metrics ---

    @Test
    void should_passReferenceInPromptContext_when_evaluatingFactualCorrectness() {
        AtomicReference<PromptContext> capturedCtx = new AtomicReference<>();
        String json = "{\"score\": 0.9, \"precision\": 0.9, \"recall\": 0.9,"
                + " \"rationale\": \"OK.\", \"statements\": []}";
        registerHandler(ollamaJsonResponse(json));

        OllamaJudge customJudge = new OllamaJudge(MODEL, "localhost", port,
                Map.of(MetricType.FACTUAL_CORRECTNESS, ctx -> {
                    capturedCtx.set(ctx);
                    return "CUSTOM_FACTUAL";
                }));

        customJudge.evaluateFactualCorrectness(QUESTION, ANSWER, REFERENCE);

        assertThat(capturedCtx.get()).isNotNull();
        assertThat(capturedCtx.get().reference()).contains(REFERENCE);
    }

    @Test
    void should_useCustomTemplate_when_configuredForToolTrajectory() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        String json = "{\"score\": 0.9, \"rationale\": \"OK.\", \"statements\": []}";
        registerCapturingHandler(ollamaJsonResponse(json), capturedBody);

        OllamaJudge customJudge = new OllamaJudge(MODEL, "localhost", port,
                Map.of(MetricType.TOOL_TRAJECTORY, ctx -> "CUSTOM_TRAJECTORY_PROMPT"));

        customJudge.evaluateToolTrajectory(QUESTION, TRAJECTORY, ANSWER);

        assertThat(capturedBody.get()).contains("CUSTOM_TRAJECTORY_PROMPT");
    }

    // --- PromptContext.forGeneration (custom GENERATION template) ---

    @Test
    void should_callCustomTemplate_with_correctGenerationPromptContext() {
        AtomicReference<PromptContext> capturedCtx = new AtomicReference<>();
        String json = "{\"score\": 0.9, \"rationale\": \"OK.\", \"statements\": []}";
        registerHandler(ollamaJsonResponse(json));

        OllamaJudge customJudge = new OllamaJudge(MODEL, "localhost", port,
                Map.of(MetricType.GENERATION, ctx -> {
                    capturedCtx.set(ctx);
                    return "CUSTOM_GENERATION";
                }));

        customJudge.evaluateGeneration(QUESTION, CONTEXT, ANSWER);

        assertThat(capturedCtx.get()).isNotNull();
        assertThat(capturedCtx.get().answer()).isPresent();
        assertThat(capturedCtx.get().answer().get()).isEqualTo(ANSWER);
    }
}
