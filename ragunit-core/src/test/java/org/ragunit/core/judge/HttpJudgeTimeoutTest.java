package org.ragunit.core.judge;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.Verdict;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The request timeout is configurable.
 *
 * <p>A local judge model is slow: a 14B model answering a faithfulness query on a laptop
 * routinely needs more than a minute. With a fixed timeout the caller cannot adapt, and
 * every call fails — which reads as a quality collapse rather than as an outage.
 *
 * <p>The mock server runs in-process and answers after a controlled delay, so the timeout
 * is verified by behaviour rather than by reading a field.
 */
class HttpJudgeTimeoutTest {

    private static final String MODEL = "llama3:8b";
    private static final int HTTP_OK = 200;
    private static final Question QUESTION = new Question("What is the capital of France?");
    private static final List<Document> CONTEXT =
            List.of(new Document("Paris is the capital of France."));
    private static final String RESPONSE = """
            {"message":{"role":"assistant","content":"{\\"score\\": 0.9, \
            \\"rationale\\": \\"Highly relevant.\\", \\"statements\\": []}"}}""";

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

    private void registerHandlerAnsweringAfter(Duration delay) {
        mockServer.createContext("/api/chat", exchange -> {
            try {
                Thread.sleep(delay.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = RESPONSE.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(HTTP_OK, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        mockServer.start();
    }

    private OllamaJudge judgeWithTimeout(Duration timeout) {
        return OllamaJudge.builder()
                .model(MODEL)
                .host("localhost")
                .port(port)
                .timeout(timeout)
                .build();
    }

    @Test
    void should_failFast_when_theServerAnswersAfterTheConfiguredTimeout() {
        registerHandlerAnsweringAfter(Duration.ofSeconds(3));
        OllamaJudge judge = judgeWithTimeout(Duration.ofMillis(300));

        long startedAt = System.nanoTime();
        assertThatThrownBy(() -> judge.evaluateRetrieval(QUESTION, CONTEXT))
                .isInstanceOf(JudgeException.class);

        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);
        assertThat(elapsed)
                .as("the configured timeout must apply, not the default one")
                .isLessThan(Duration.ofSeconds(2));
    }

    @Test
    void should_returnVerdict_when_theServerAnswersWithinTheConfiguredTimeout() {
        registerHandlerAnsweringAfter(Duration.ofMillis(200));
        OllamaJudge judge = judgeWithTimeout(Duration.ofSeconds(10));

        Verdict verdict = judge.evaluateRetrieval(QUESTION, CONTEXT);

        assertThat(verdict.rationale()).isEqualTo("Highly relevant.");
    }

    @Test
    void should_rejectTheTimeout_when_itIsZeroOrNegative() {
        OllamaJudge.Builder builder = OllamaJudge.builder().model(MODEL);

        assertThatThrownBy(() -> builder.timeout(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.timeout(Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_keepSixtySeconds_when_noTimeoutIsConfigured() {
        assertThat(HttpJudge.DEFAULT_TIMEOUT).isEqualTo(Duration.ofSeconds(60));
    }
}
