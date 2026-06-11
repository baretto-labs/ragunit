package org.ragunit.core.testset;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.Testset;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for OllamaTestsetGenerator using a JDK-native HTTP mock server.
 */
class OllamaTestsetGeneratorTest {

    private static final String MODEL = "llama3:8b";
    private static final int HTTP_OK = 200;
    private static final Document DOC_A = new Document("Paris is the capital of France.");
    private static final Document DOC_B = new Document("The Eiffel Tower was built in 1889.");
    private static final List<Document> CORPUS = List.of(DOC_A, DOC_B);

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

    private static String ollamaJsonResponse(String json) {
        String escaped = json.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"message\":{\"role\":\"assistant\",\"content\":\"" + escaped + "\"}}";
    }

    private OllamaTestsetGenerator generator() {
        return new OllamaTestsetGenerator(MODEL, "localhost", port);
    }

    @Test
    void should_returnTestset_when_corpusHasDocuments() {
        registerHandler(ollamaJsonResponse(
                "{\"question\": \"What is the capital of France?\","
                + " \"referenceAnswer\": \"Paris.\"}"));

        Testset testset = generator().generate(CORPUS, 2);

        assertThat(testset.size()).isEqualTo(2);
    }

    @Test
    void should_includeDocumentAsContext_when_generatingTestCase() {
        registerHandler(ollamaJsonResponse(
                "{\"question\": \"What is the capital of France?\","
                + " \"referenceAnswer\": \"Paris.\"}"));

        Testset testset = generator().generate(List.of(DOC_A), 1);

        assertThat(testset.cases().get(0).context()).containsExactly(DOC_A);
    }

    @Test
    void should_returnEmptyTestset_when_countIsZero() {
        mockServer.start();

        Testset testset = generator().generate(CORPUS, 0);

        assertThat(testset.isEmpty()).isTrue();
    }

    @Test
    void should_populateQuestionAndReference_when_judgeReturnsValidJson() {
        registerHandler(ollamaJsonResponse(
                "{\"question\": \"When was the Eiffel Tower built?\","
                + " \"referenceAnswer\": \"1889.\"}"));

        Testset testset = generator().generate(List.of(DOC_B), 1);

        assertThat(testset.cases().get(0).question().text())
                .isEqualTo("When was the Eiffel Tower built?");
        assertThat(testset.cases().get(0).referenceAnswer().text())
                .isEqualTo("1889.");
    }

    @Test
    void should_throwException_when_corpusIsEmpty() {
        mockServer.start();

        assertThatThrownBy(() -> generator().generate(List.of(), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("corpus");
    }

    @Test
    void should_throwGeneratorException_when_serverIsUnreachable() {
        OllamaTestsetGenerator unreachable = new OllamaTestsetGenerator(MODEL, "localhost", 1);

        assertThatThrownBy(() -> unreachable.generate(CORPUS, 1))
                .isInstanceOf(TestsetGeneratorException.class);
    }

    @Test
    void should_includeDocumentContentInPrompt_when_generating() {
        var capturedBody = new java.util.concurrent.atomic.AtomicReference<String>();
        mockServer.createContext("/api/chat", exchange -> {
            try (InputStream in = exchange.getRequestBody()) {
                capturedBody.set(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
            String resp = ollamaJsonResponse(
                    "{\"question\": \"Q?\", \"referenceAnswer\": \"A.\"}");
            byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(HTTP_OK, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        mockServer.start();

        generator().generate(List.of(DOC_A), 1);

        assertThat(capturedBody.get()).contains("Paris is the capital of France.");
    }

    @Test
    void should_degradeGracefully_when_responseIsNotValidJson() {
        registerHandler(ollamaJsonResponse("I cannot generate a question right now."));

        Testset testset = generator().generate(List.of(DOC_A), 1);

        // Fallback: one case with empty/default values rather than crashing
        assertThat(testset.size()).isEqualTo(1);
        assertThat(testset.cases().get(0).context()).containsExactly(DOC_A);
    }
}
