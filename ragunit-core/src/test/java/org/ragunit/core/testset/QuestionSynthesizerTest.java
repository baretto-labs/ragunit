package org.ragunit.core.testset;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.QuestionType;
import org.ragunit.core.domain.TestCase;
import org.ragunit.core.domain.Testset;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests for SimpleQuestionSynthesizer, MultiHopQuestionSynthesizer, and distribution. */
class QuestionSynthesizerTest {

    private static final String MODEL = "llama3:8b";
    private static final int HTTP_OK = 200;
    private static final Document DOC_A = new Document("Paris is the capital of France.");
    private static final Document DOC_B = new Document("The Eiffel Tower was built in 1889.");
    private static final Document DOC_C = new Document("France borders Spain to the south.");
    private static final List<Document> CORPUS = List.of(DOC_A, DOC_B, DOC_C);

    private HttpServer mockServer;
    private int port;
    private ExecutorService serverExecutor;

    @BeforeEach
    void startMockServer() throws IOException {
        serverExecutor = Executors.newSingleThreadExecutor();
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        mockServer.setExecutor(serverExecutor);
        port = mockServer.getAddress().getPort();
    }

    @AfterEach
    void stopMockServer() throws InterruptedException {
        mockServer.stop(0);
        serverExecutor.shutdown();
        serverExecutor.awaitTermination(2, TimeUnit.SECONDS);
    }

    private void registerHandler(String responseBody) {
        mockServer.createContext("/api/chat", exchange -> {
            try (InputStream in = exchange.getRequestBody()) {
                in.readAllBytes();
            }
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(HTTP_OK, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        mockServer.start();
    }

    private static String ollamaResponse(String json) {
        String escaped = json.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"message\":{\"role\":\"assistant\",\"content\":\"" + escaped + "\"}}";
    }

    // --- QuestionType ---

    @Test
    void should_haveSimpleAndMultiHopTypes() {
        assertThat(QuestionType.values()).contains(QuestionType.SIMPLE, QuestionType.MULTI_HOP);
    }

    // --- TestCase.questionType ---

    @Test
    void should_defaultToSimple_when_creatingTestCaseWithFactoryMethod() {
        TestCase tc = TestCase.simple(
                new org.ragunit.core.domain.Question("What?"),
                List.of(DOC_A),
                new org.ragunit.core.domain.ReferenceAnswer("Answer."));
        assertThat(tc.questionType()).isEqualTo(QuestionType.SIMPLE);
    }

    @Test
    void should_setMultiHop_when_creatingTestCaseWithMultiHopFactory() {
        TestCase tc = TestCase.multiHop(
                new org.ragunit.core.domain.Question("What?"),
                List.of(DOC_A, DOC_B),
                new org.ragunit.core.domain.ReferenceAnswer("Answer."));
        assertThat(tc.questionType()).isEqualTo(QuestionType.MULTI_HOP);
    }

    // --- SimpleQuestionSynthesizer ---

    @Test
    void should_generateOneQuestionPerDocument_when_synthesizingSimple() {
        registerHandler(ollamaResponse(
                "{\"question\": \"What is the capital?\", \"referenceAnswer\": \"Paris.\"}"));

        var synthesizer = new SimpleQuestionSynthesizer(MODEL, "localhost", port);
        List<TestCase> cases = synthesizer.synthesize(CORPUS, 3);

        assertThat(cases).hasSize(3);
    }

    @Test
    void should_tagCasesAsSimple_when_usingSynthesizer() {
        registerHandler(ollamaResponse(
                "{\"question\": \"What is the capital?\", \"referenceAnswer\": \"Paris.\"}"));

        var synthesizer = new SimpleQuestionSynthesizer(MODEL, "localhost", port);
        List<TestCase> cases = synthesizer.synthesize(List.of(DOC_A), 1);

        assertThat(cases.get(0).questionType()).isEqualTo(QuestionType.SIMPLE);
    }

    @Test
    void should_limitToCorpusSize_when_countExceedsCorpus() {
        registerHandler(ollamaResponse(
                "{\"question\": \"Q?\", \"referenceAnswer\": \"A.\"}"));

        var synthesizer = new SimpleQuestionSynthesizer(MODEL, "localhost", port);
        List<TestCase> cases = synthesizer.synthesize(List.of(DOC_A), 10);

        assertThat(cases).hasSize(1);
    }

    @Test
    void should_includeSingleDocInContext_when_simpleQuestion() {
        registerHandler(ollamaResponse(
                "{\"question\": \"What is the capital?\", \"referenceAnswer\": \"Paris.\"}"));

        var synthesizer = new SimpleQuestionSynthesizer(MODEL, "localhost", port);
        List<TestCase> cases = synthesizer.synthesize(List.of(DOC_A), 1);

        assertThat(cases.get(0).context()).containsExactly(DOC_A);
    }

    // --- MultiHopQuestionSynthesizer ---

    @Test
    void should_combineMultipleDocsInContext_when_multiHop() {
        registerHandler(ollamaResponse(
                "{\"question\": \"What city hosts this tower?\", \"referenceAnswer\": \"Paris.\"}"));

        var synthesizer = new MultiHopQuestionSynthesizer(MODEL, "localhost", port);
        List<TestCase> cases = synthesizer.synthesize(CORPUS, 1);

        assertThat(cases.get(0).context().size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void should_tagCasesAsMultiHop_when_usingMultiHopSynthesizer() {
        registerHandler(ollamaResponse(
                "{\"question\": \"Q?\", \"referenceAnswer\": \"A.\"}"));

        var synthesizer = new MultiHopQuestionSynthesizer(MODEL, "localhost", port);
        List<TestCase> cases = synthesizer.synthesize(CORPUS, 1);

        assertThat(cases.get(0).questionType()).isEqualTo(QuestionType.MULTI_HOP);
    }

    @Test
    void should_throwException_when_corpusHasFewerThanTwoDocuments() {
        mockServer.start();
        var synthesizer = new MultiHopQuestionSynthesizer(MODEL, "localhost", port);

        assertThatThrownBy(() -> synthesizer.synthesize(List.of(DOC_A), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2");
    }

    @Test
    void should_includeMultipleDocsInPrompt_when_multiHopSynthesizing() {
        AtomicReference<String> captured = new AtomicReference<>();
        mockServer.createContext("/api/chat", exchange -> {
            try (InputStream in = exchange.getRequestBody()) {
                captured.set(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
            String resp = ollamaResponse("{\"question\": \"Q?\", \"referenceAnswer\": \"A.\"}");
            byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(HTTP_OK, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        mockServer.start();

        new MultiHopQuestionSynthesizer(MODEL, "localhost", port).synthesize(CORPUS, 1);

        assertThat(captured.get()).contains("Paris is the capital of France.");
        assertThat(captured.get()).contains("The Eiffel Tower was built in 1889.");
    }

    // --- OllamaTestsetGenerator with distribution ---

    @Test
    void should_useSimpleSynthesizer_when_onlySimpleTypeConfigured() {
        registerHandler(ollamaResponse(
                "{\"question\": \"What is the capital?\", \"referenceAnswer\": \"Paris.\"}"));

        OllamaTestsetGenerator generator = new OllamaTestsetGenerator(MODEL, "localhost", port,
                Map.of(QuestionType.SIMPLE, 1.0));
        Testset testset = generator.generate(CORPUS, 2);

        assertThat(testset.cases())
                .allMatch(tc -> tc.questionType() == QuestionType.SIMPLE);
    }

    @Test
    void should_respectDistribution_when_mixedTypesRequested() {
        registerHandler(ollamaResponse(
                "{\"question\": \"Q?\", \"referenceAnswer\": \"A.\"}"));

        // 2 SIMPLE, 1 MULTI_HOP out of 3 total
        OllamaTestsetGenerator generator = new OllamaTestsetGenerator(MODEL, "localhost", port,
                Map.of(QuestionType.SIMPLE, 0.67, QuestionType.MULTI_HOP, 0.33));
        Testset testset = generator.generate(CORPUS, 3);

        assertThat(testset.size()).isEqualTo(3);
        long simpleCount = testset.cases().stream()
                .filter(tc -> tc.questionType() == QuestionType.SIMPLE).count();
        long multiHopCount = testset.cases().stream()
                .filter(tc -> tc.questionType() == QuestionType.MULTI_HOP).count();
        assertThat(simpleCount).isGreaterThanOrEqualTo(1);
        assertThat(multiHopCount).isGreaterThanOrEqualTo(1);
    }
}
