package org.ragunit.cloud;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ragunit.core.domain.Answer;
import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.ReferenceAnswer;
import org.ragunit.core.domain.Verdict;
import org.ragunit.core.judge.Criterion;
import org.ragunit.core.judge.JudgeException;
import org.ragunit.core.judge.JudgeQuery;
import org.ragunit.core.judge.JudgeResult;
import org.ragunit.core.judge.MetricType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link OpenAiCompatibleJudge} using a JDK-native HTTP server to mock any
 * OpenAI-compatible Chat Completions endpoint. No external process required.
 */
class OpenAiCompatibleJudgeTest {

    private static final String MODEL = "gpt-4o-mini";
    private static final double SCORE_09 = 0.9;
    private static final int HTTP_OK = 200;
    private static final Question QUESTION = new Question("What is the capital of France?");
    private static final List<Document> CONTEXT = List.of(new Document("Paris is the capital of France."));
    private static final Answer ANSWER = new Answer("Paris.");
    private static final ReferenceAnswer REFERENCE = new ReferenceAnswer("The capital of France is Paris.");

    private HttpServer mockServer;
    private int port;
    private final AtomicReference<String> capturedBody = new AtomicReference<>();
    private final AtomicReference<String> capturedAuth = new AtomicReference<>();
    private final AtomicReference<String> capturedPath = new AtomicReference<>();

    @BeforeEach
    void startMockServer() throws IOException {
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        port = mockServer.getAddress().getPort();
    }

    @AfterEach
    void stopMockServer() {
        mockServer.stop(0);
    }

    private void register(String chatContent) {
        registerRaw(openAiResponse(chatContent));
    }

    private void registerRaw(String responseBody) {
        mockServer.createContext("/v1/chat/completions", exchange -> {
            capturedPath.set(exchange.getRequestURI().getPath());
            capturedAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            try (InputStream in = exchange.getRequestBody()) {
                capturedBody.set(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(HTTP_OK, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        mockServer.start();
    }

    /** Wraps the judge's reply as an OpenAI Chat Completions response, with a trailing key after content. */
    private static String openAiResponse(String chatContent) {
        String escaped = chatContent.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"id\":\"chatcmpl-1\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\","
                + "\"content\":\"" + escaped + "\",\"refusal\":null},\"finish_reason\":\"stop\"}]}";
    }

    private OpenAiCompatibleJudge judge() {
        return OpenAiCompatibleJudge.builder()
                .baseUrl("http://localhost:" + port + "/v1")
                .apiKey("sk-test-123")
                .model(MODEL)
                .build();
    }

    // --- request shape ---

    @Test
    void should_postToChatCompletions_when_evaluating() {
        register("{\"score\": 0.9, \"rationale\": \"Relevant.\", \"statements\": []}");

        judge().evaluateRetrieval(QUESTION, CONTEXT);

        assertThat(capturedPath.get()).isEqualTo("/v1/chat/completions");
    }

    @Test
    void should_sendBearerAuthorizationHeader_when_apiKeyConfigured() {
        register("{\"score\": 0.9, \"rationale\": \"Relevant.\", \"statements\": []}");

        judge().evaluateRetrieval(QUESTION, CONTEXT);

        assertThat(capturedAuth.get()).isEqualTo("Bearer sk-test-123");
    }

    @Test
    void should_omitAuthorizationHeader_when_noApiKey() {
        register("{\"score\": 0.9, \"rationale\": \"Relevant.\", \"statements\": []}");

        OpenAiCompatibleJudge keyless = OpenAiCompatibleJudge.builder()
                .baseUrl("http://localhost:" + port + "/v1")
                .model(MODEL)
                .build();

        keyless.evaluateRetrieval(QUESTION, CONTEXT);

        assertThat(capturedAuth.get()).isNull();
    }

    @Test
    void should_includeModelMessagesAndTopLevelTemperature_when_evaluating() {
        register("{\"score\": 0.9, \"rationale\": \"Relevant.\", \"statements\": []}");

        judge().evaluateRetrieval(QUESTION, CONTEXT);

        assertThat(capturedBody.get()).contains("\"model\":\"gpt-4o-mini\"");
        assertThat(capturedBody.get()).contains("\"messages\"");
        assertThat(capturedBody.get()).contains("\"temperature\":0.0");
        assertThat(capturedBody.get()).contains(QUESTION.text());
    }

    @Test
    void should_sendCustomTemperature_when_configured() {
        register("{\"score\": 0.9, \"rationale\": \"ok\", \"statements\": []}");

        OpenAiCompatibleJudge warm = OpenAiCompatibleJudge.builder()
                .baseUrl("http://localhost:" + port + "/v1")
                .apiKey("k")
                .model(MODEL)
                .temperature(0.7)
                .build();

        warm.evaluateRetrieval(QUESTION, CONTEXT);

        assertThat(capturedBody.get()).contains("\"temperature\":0.7");
    }

    // --- response parsing ---

    @Test
    void should_extractContentFromChoices_when_responseHasTrailingKeysAfterContent() {
        register("{\"score\": 0.9, \"rationale\": \"Highly relevant.\", \"statements\": []}");

        Verdict verdict = judge().evaluateRetrieval(QUESTION, CONTEXT);

        assertThat(verdict.score().value()).isEqualTo(SCORE_09);
        assertThat(verdict.rationale()).isEqualTo("Highly relevant.");
        assertThat(verdict.model()).isEqualTo(MODEL);
    }

    @Test
    void should_attachPromptAndRawResponse_when_evaluating() {
        register("{\"score\": 0.9, \"rationale\": \"ok\", \"statements\": []}");

        Verdict verdict = judge().evaluateRetrieval(QUESTION, CONTEXT);

        assertThat(verdict.promptUsed()).hasValueSatisfying(p -> assertThat(p).contains(QUESTION.text()));
        assertThat(verdict.rawResponse()).hasValueSatisfying(r -> assertThat(r).contains("\"score\": 0.9"));
    }

    @Test
    void should_parseStatements_when_evaluatingGeneration() {
        register("{\"score\": 0.75, \"rationale\": \"Mostly faithful.\", \"statements\": ["
                + "{\"text\": \"Paris is the capital.\", \"supported\": true},"
                + "{\"text\": \"Paris has 10M people.\", \"supported\": false}]}");

        Verdict verdict = judge().evaluateGeneration(QUESTION, CONTEXT, ANSWER);

        assertThat(verdict.statements()).hasSize(2);
        assertThat(verdict.statements().get(0).supported()).isTrue();
    }

    @Test
    void should_throwJudgeException_when_serverUnreachable() {
        OpenAiCompatibleJudge unreachable = OpenAiCompatibleJudge.builder()
                .baseUrl("http://localhost:1/v1")
                .model(MODEL)
                .build();

        assertThatThrownBy(() -> unreachable.evaluateRetrieval(QUESTION, CONTEXT))
                .isInstanceOf(JudgeException.class);
    }

    // --- generic JudgeQuery (non-RAG) ---

    @Test
    void should_evaluateCustomCriterionQuery_when_usingGenericApi() {
        register("{\"score\": 0.8, \"rationale\": \"Concise.\", \"statements\": []}");

        JudgeQuery query = JudgeQuery.builder()
                .criterion(Criterion.of("conciseness", "Is the summary concise and faithful?"))
                .input("Source", "A long article.")
                .input("Summary", "Short.")
                .build();

        JudgeResult result = judge().evaluate(query);

        assertThat(result.score()).isEqualTo(0.8);
        assertThat(capturedBody.get()).contains("Is the summary concise and faithful?");
    }

    @Test
    void should_dispatchBuiltInMetricQuery_when_criterionIsMetricType() {
        register("{\"score\": 0.9, \"rationale\": \"ok\", \"statements\": []}");

        JudgeQuery query = JudgeQuery.builder()
                .criterion(MetricType.RETRIEVAL)
                .input(JudgeQuery.INPUT_QUESTION, QUESTION.text())
                .input(JudgeQuery.INPUT_CONTEXT, List.of(CONTEXT.get(0).content()))
                .build();

        judge().evaluate(query);

        assertThat(capturedBody.get()).contains("Rate how relevant");
    }

    @Test
    void should_evaluateFactualCorrectness_when_referenceProvided() {
        register("{\"score\": 0.9, \"precision\": 0.95, \"recall\": 0.85,"
                + " \"rationale\": \"Strong overlap.\", \"statements\": []}");

        assertThat(judge().evaluateFactualCorrectness(QUESTION, ANSWER, REFERENCE)).isNotNull();
    }

    // --- builder validation ---

    @Test
    void should_throwNullPointerException_when_builderHasNoBaseUrl() {
        assertThatThrownBy(() -> OpenAiCompatibleJudge.builder().model(MODEL).build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("baseUrl");
    }

    @Test
    void should_throwNullPointerException_when_builderHasNoModel() {
        assertThatThrownBy(() -> OpenAiCompatibleJudge.builder().baseUrl("http://x/v1").build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("model");
    }

    @Test
    void should_stripTrailingSlashFromBaseUrl_when_building() {
        register("{\"score\": 0.9, \"rationale\": \"ok\", \"statements\": []}");

        OpenAiCompatibleJudge trailing = OpenAiCompatibleJudge.builder()
                .baseUrl("http://localhost:" + port + "/v1/")
                .model(MODEL)
                .build();

        trailing.evaluateRetrieval(QUESTION, CONTEXT);

        assertThat(capturedPath.get()).isEqualTo("/v1/chat/completions");
    }

    @Test
    void should_useCustomFaithfulnessPrompt_when_configuredViaBuilder() {
        register("{\"score\": 0.9, \"rationale\": \"ok\", \"statements\": []}");

        OpenAiCompatibleJudge custom = OpenAiCompatibleJudge.builder()
                .baseUrl("http://localhost:" + port + "/v1")
                .model(MODEL)
                .faithfulnessPrompt(ctx -> "CUSTOM_FAITHFULNESS for " + ctx.question().text())
                .build();

        custom.evaluateGeneration(QUESTION, CONTEXT, ANSWER);

        assertThat(capturedBody.get()).contains("CUSTOM_FAITHFULNESS for");
    }
}
