package org.ragunit.core.testset;

import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.ReferenceAnswer;
import org.ragunit.core.domain.TestCase;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared HTTP client used by {@link SimpleQuestionSynthesizer} and
 * {@link MultiHopQuestionSynthesizer} to call the Ollama API.
 *
 * <p>Package-private — not part of the public API.
 */
final class OllamaTestsetClient {

    private static final Pattern QUESTION_PATTERN =
            Pattern.compile("\"question\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern REFERENCE_PATTERN =
            Pattern.compile("\"referenceAnswer\"\\s*:\\s*\"([^\"]+)\"");
    private static final String FALLBACK_QUESTION = "Could not generate question.";
    private static final String FALLBACK_REFERENCE = "Could not generate reference answer.";

    /** Request timeout in seconds. Testset generation prompts can be long. */
    private static final int REQUEST_TIMEOUT_SECONDS = 60;

    /** Maximum accepted response body size (1 MB). Guards against runaway responses. */
    private static final int MAX_RESPONSE_BYTES = 1024 * 1024;

    private final String model;
    private final String baseUrl;
    private final HttpClient httpClient;

    OllamaTestsetClient(String model, String host, int port) {
        this.model = Objects.requireNonNull(model, "model");
        this.baseUrl = "http://" + Objects.requireNonNull(host, "host") + ":" + port;
        this.httpClient = HttpClient.newHttpClient();
    }

    TestCase callAndParse(String prompt, java.util.List<Document> context,
                          org.ragunit.core.domain.QuestionType type) {
        String content = call(prompt);
        Matcher qm = QUESTION_PATTERN.matcher(content);
        Matcher rm = REFERENCE_PATTERN.matcher(content);
        String questionText = qm.find() ? qm.group(1) : FALLBACK_QUESTION;
        String referenceText = rm.find() ? rm.group(1) : FALLBACK_REFERENCE;
        return new TestCase(
                new Question(questionText),
                context,
                new ReferenceAnswer(referenceText),
                type);
    }

    String call(String prompt) {
        String requestBody = buildRequestBody(prompt);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/chat"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request, limitedBodyHandler());
            return extractContent(response.body());
        } catch (IOException e) {
            throw new TestsetGeneratorException("Ollama call failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TestsetGeneratorException("Ollama call interrupted", e);
        }
    }

    private static HttpResponse.BodyHandler<String> limitedBodyHandler() {
        return info -> HttpResponse.BodySubscribers.mapping(
                HttpResponse.BodySubscribers.ofByteArray(),
                bytes -> {
                    if (bytes.length > MAX_RESPONSE_BYTES) {
                        throw new TestsetGeneratorException(
                                "Ollama response exceeds size limit (%d bytes)".formatted(MAX_RESPONSE_BYTES), null);
                    }
                    return new String(bytes, StandardCharsets.UTF_8);
                });
    }

    private String buildRequestBody(String prompt) {
        return "{\"model\":\"" + escapeJson(model) + "\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\""
                + escapeJson(prompt) + "\"}],"
                + "\"stream\":false}";
    }

    private static String extractContent(String responseBody) {
        int contentKey = responseBody.indexOf("\"content\":\"");
        if (contentKey == -1) {
            throw new TestsetGeneratorException(
                    "Failed to parse Ollama response: missing 'content' key", null);
        }
        int start = contentKey + "\"content\":\"".length();
        int end = responseBody.lastIndexOf("\"}");
        if (end <= start) {
            throw new TestsetGeneratorException(
                    "Failed to parse Ollama response: unterminated content", null);
        }
        return responseBody.substring(start, end)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private static String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
