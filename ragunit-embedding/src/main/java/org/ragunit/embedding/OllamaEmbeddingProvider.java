package org.ragunit.embedding;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link EmbeddingProvider} that calls the Ollama embeddings API.
 *
 * <p>Sends a {@code POST /api/embeddings} request and parses the {@code "embedding"} array
 * from the JSON response. Uses the JDK built-in {@link HttpClient} — zero extra dependencies.
 *
 * <p>Recommended model: {@code nomic-embed-text} (768 dimensions, fast, accurate).
 * Pull with: {@code ollama pull nomic-embed-text}
 */
public final class OllamaEmbeddingProvider implements EmbeddingProvider {

    /** Default Ollama host. */
    static final String DEFAULT_HOST = "localhost";

    /** Default Ollama port. */
    static final int DEFAULT_PORT = 11434;

    private static final int HTTP_OK = 200;
    private static final Pattern EMBEDDING_ARRAY_PATTERN =
            Pattern.compile("\"embedding\"\\s*:\\s*\\[([^]]*)]");
    private static final Pattern NUMBER_PATTERN =
            Pattern.compile("[-+]?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?");

    private final String model;
    private final String host;
    private final int port;
    private final HttpClient httpClient;

    /**
     * Creates a provider using the default Ollama host ({@code localhost:11434}).
     *
     * @param model the Ollama embedding model name (e.g. {@code nomic-embed-text})
     */
    public OllamaEmbeddingProvider(String model) {
        this(model, DEFAULT_HOST, DEFAULT_PORT);
    }

    /**
     * Creates a provider connected to a custom Ollama endpoint.
     *
     * @param model the Ollama embedding model name
     * @param host  the Ollama server hostname
     * @param port  the Ollama server port
     */
    public OllamaEmbeddingProvider(String model, String host, int port) {
        this.model = Objects.requireNonNull(model, "model");
        this.host = host;
        this.port = port;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public float[] embed(String text) {
        Objects.requireNonNull(text, "text");
        String requestBody = buildRequestBody(text);
        String responseBody = callApi(requestBody);
        return parseEmbedding(responseBody);
    }

    private String buildRequestBody(String text) {
        return "{\"model\":\"" + escapeJson(model) + "\",\"prompt\":\"" + escapeJson(text) + "\"}";
    }

    private String callApi(String requestBody) {
        HttpRequest request = buildHttpRequest(requestBody);
        try {
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != HTTP_OK) {
                throw new EmbeddingException(
                        "Ollama embeddings API returned HTTP " + response.statusCode());
            }
            return response.body();
        } catch (IOException e) {
            throw new EmbeddingException("Failed to call Ollama embeddings API: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EmbeddingException("Ollama embeddings API call interrupted", e);
        }
    }

    private HttpRequest buildHttpRequest(String requestBody) {
        URI uri = URI.create("http://" + host + ":" + port + "/api/embeddings");
        return HttpRequest.newBuilder(uri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    private static float[] parseEmbedding(String responseBody) {
        Matcher arrayMatcher = EMBEDDING_ARRAY_PATTERN.matcher(responseBody);
        if (!arrayMatcher.find()) {
            throw new EmbeddingException("No 'embedding' array found in Ollama response");
        }
        String arrayContent = arrayMatcher.group(1);
        List<Float> values = new ArrayList<>();
        Matcher numMatcher = NUMBER_PATTERN.matcher(arrayContent);
        while (numMatcher.find()) {
            values.add(Float.parseFloat(numMatcher.group()));
        }
        if (values.isEmpty()) {
            throw new EmbeddingException("Empty embedding array in Ollama response");
        }
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
