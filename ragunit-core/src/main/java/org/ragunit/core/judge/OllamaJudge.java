package org.ragunit.core.judge;

import org.ragunit.core.domain.Answer;
import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.FactualCorrectnessVerdict;
import org.ragunit.core.domain.PromptContext;
import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.ReferenceAnswer;
import org.ragunit.core.domain.ToolCall;
import org.ragunit.core.domain.Verdict;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@link RagJudge} implementation that delegates scoring to a local Ollama model.
 *
 * <p>Calls {@code POST /api/chat} with a prompt that instructs the model to reply
 * with a structured JSON object containing {@code score}, {@code rationale}, and
 * {@code statements} (RAGAS-style claim decomposition for faithfulness evaluation).
 *
 * <p>Every prompt comes from {@link JudgePromptLibrary} — visible, versioned, and
 * overridable per metric via {@link #builder()} or the template-map constructors.
 *
 * <p>Uses the JDK built-in {@link HttpClient} — zero extra dependencies.
 * Any network or parse failure is wrapped in a {@link JudgeException}.
 * The response is parsed by {@link VerdictParser}, which degrades gracefully on
 * malformed JSON rather than throwing.
 */
public final class OllamaJudge implements RagJudge {

    /** Default host when none is specified. */
    static final String DEFAULT_HOST = "localhost";

    /** Default Ollama port when none is specified. */
    static final int DEFAULT_PORT = 11434;

    /** Request timeout in seconds. LLM inference on large models can be slow. */
    private static final int REQUEST_TIMEOUT_SECONDS = 60;

    /** Maximum accepted response body size (1 MB). Guards against runaway responses. */
    private static final int MAX_RESPONSE_BYTES = 1024 * 1024;

    private final String model;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final Map<MetricType, JudgePromptTemplate> templates;

    /**
     * Creates an OllamaJudge connecting to {@code localhost:11434} with default prompts.
     *
     * @param model the Ollama model name, e.g. {@code "qwen2.5:14b"}
     */
    public OllamaJudge(String model) {
        this(model, DEFAULT_HOST, DEFAULT_PORT);
    }

    /**
     * Creates an OllamaJudge with custom prompt templates for specific metrics.
     *
     * @param model     the Ollama model name, e.g. {@code "qwen2.5:14b"}
     * @param templates custom templates keyed by metric type; missing keys fall back to defaults
     */
    public OllamaJudge(String model, Map<MetricType, JudgePromptTemplate> templates) {
        this(model, DEFAULT_HOST, DEFAULT_PORT, templates);
    }

    /**
     * Creates an OllamaJudge connecting to a custom host and port with default prompts.
     *
     * @param model the Ollama model name, e.g. {@code "qwen2.5:14b"}
     * @param host  the Ollama server hostname or IP, e.g. {@code "192.168.1.10"}
     * @param port  the Ollama server port, e.g. {@code 11434}
     */
    public OllamaJudge(String model, String host, int port) {
        this(model, host, port, Map.of());
    }

    /**
     * Creates an OllamaJudge connecting to a custom host and port with custom prompt templates.
     *
     * @param model     the Ollama model name, e.g. {@code "qwen2.5:14b"}
     * @param host      the Ollama server hostname or IP, e.g. {@code "192.168.1.10"}
     * @param port      the Ollama server port, e.g. {@code 11434}
     * @param templates custom templates keyed by metric type; missing keys fall back to defaults
     */
    public OllamaJudge(String model, String host, int port, Map<MetricType, JudgePromptTemplate> templates) {
        this.model = Objects.requireNonNull(model, "model");
        this.baseUrl = "http://" + Objects.requireNonNull(host, "host") + ":" + port;
        this.httpClient = HttpClient.newHttpClient();
        this.templates = mergeWithDefaults(Objects.requireNonNull(templates, "templates"));
    }

    /**
     * Starts a fluent builder — the recommended way to configure an OllamaJudge.
     *
     * <pre>{@code
     * OllamaJudge judge = OllamaJudge.builder()
     *         .model("qwen2.5:14b")
     *         .faithfulnessPrompt(ctx -> "...")
     *         .build();
     * }</pre>
     *
     * @return a new {@link Builder} with default host, port, and prompts
     */
    public static Builder builder() {
        return new Builder();
    }

    private static Map<MetricType, JudgePromptTemplate> mergeWithDefaults(
            Map<MetricType, JudgePromptTemplate> overrides) {
        Map<MetricType, JudgePromptTemplate> merged = new EnumMap<>(JudgePromptLibrary.defaults());
        merged.putAll(overrides);
        return Map.copyOf(merged);
    }

    @Override
    public Verdict evaluateRetrieval(Question question, List<Document> context) {
        return callOllama(render(MetricType.RETRIEVAL, PromptContext.forRetrieval(question, context)));
    }

    @Override
    public Verdict evaluateGeneration(Question question, List<Document> context, Answer answer) {
        return callOllama(render(MetricType.GENERATION,
                PromptContext.forGeneration(question, context, answer)));
    }

    @Override
    public Verdict evaluateContextRejection(Question question, List<Document> context) {
        return callOllama(render(MetricType.CONTEXT_REJECTION,
                PromptContext.forRetrieval(question, context)));
    }

    @Override
    public Verdict evaluateRejection(Question question, List<Document> context, Answer answer) {
        return callOllama(render(MetricType.REJECTION,
                PromptContext.forGeneration(question, context, answer)));
    }

    @Override
    public Verdict evaluateContextRecall(Question question, List<Document> context, ReferenceAnswer reference) {
        return callOllama(render(MetricType.CONTEXT_RECALL,
                PromptContext.forContextRecall(question, context, reference)));
    }

    @Override
    public Verdict evaluateAnswerRelevancy(Question question, Answer answer) {
        return callOllama(render(MetricType.ANSWER_RELEVANCY,
                PromptContext.forGeneration(question, List.of(), answer)));
    }

    @Override
    public Verdict evaluateContextPrecision(Question question, List<Document> context) {
        String prompt = render(MetricType.CONTEXT_PRECISION, PromptContext.forRetrieval(question, context));
        return VerdictParser.parse(rawOllamaCall(prompt), model, context);
    }

    @Override
    public FactualCorrectnessVerdict evaluateFactualCorrectness(
            Question question, Answer answer, ReferenceAnswer reference) {
        String prompt = render(MetricType.FACTUAL_CORRECTNESS,
                PromptContext.forFactualCorrectness(question, answer, reference));
        return VerdictParser.parseFactualCorrectness(rawOllamaCall(prompt), model);
    }

    @Override
    public Verdict evaluateToolTrajectory(Question question, List<ToolCall> trajectory, Answer answer) {
        return callOllama(render(MetricType.TOOL_TRAJECTORY,
                PromptContext.forToolTrajectory(question, trajectory, answer)));
    }

    @Override
    public Verdict evaluateContextPromptInjection(Question question, List<Document> context) {
        return callOllama(render(MetricType.CONTEXT_PROMPT_INJECTION,
                PromptContext.forRetrieval(question, context)));
    }

    @Override
    public Verdict evaluatePromptInjection(Question question, List<Document> context, Answer answer) {
        return callOllama(render(MetricType.PROMPT_INJECTION,
                PromptContext.forGeneration(question, context, answer)));
    }

    @Override
    public Verdict evaluateContextPIILeak(Question question, List<Document> context) {
        return callOllama(render(MetricType.CONTEXT_PII_LEAK,
                PromptContext.forRetrieval(question, context)));
    }

    @Override
    public Verdict evaluatePIILeak(Question question, List<Document> context, Answer answer) {
        return callOllama(render(MetricType.PII_LEAK,
                PromptContext.forGeneration(question, context, answer)));
    }

    private String render(MetricType type, PromptContext ctx) {
        return templates.get(type).render(ctx);
    }

    private Verdict callOllama(String prompt) {
        return VerdictParser.parse(rawOllamaCall(prompt), model);
    }

    private String rawOllamaCall(String prompt) {
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
            throw new JudgeException("Ollama HTTP call failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JudgeException("Ollama HTTP call interrupted", e);
        }
    }

    private static HttpResponse.BodyHandler<String> limitedBodyHandler() {
        return info -> HttpResponse.BodySubscribers.mapping(
                HttpResponse.BodySubscribers.ofByteArray(),
                bytes -> {
                    if (bytes.length > MAX_RESPONSE_BYTES) {
                        throw new JudgeException(
                                "Ollama response exceeds size limit (%d bytes)".formatted(MAX_RESPONSE_BYTES));
                    }
                    return new String(bytes, StandardCharsets.UTF_8);
                });
    }

    private String buildRequestBody(String prompt) {
        return """
                {"model":"%s","messages":[{"role":"user","content":"%s"}],"stream":false}\
                """.formatted(escapeJson(model), escapeJson(prompt));
    }

    private static String extractContent(String responseBody) {
        // Parses: {"message":{"role":"...","content":"<content>"}}
        int contentKey = responseBody.indexOf("\"content\":\"");
        if (contentKey == -1) {
            throw new JudgeException("Failed to parse Ollama response: missing 'content' key");
        }
        int start = contentKey + "\"content\":\"".length();
        int end = responseBody.indexOf("\"}", start);
        if (end == -1) {
            throw new JudgeException("Failed to parse Ollama response: unterminated content value");
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

    /**
     * Fluent builder for {@link OllamaJudge}.
     *
     * <p>Only {@link #model(String)} is required. Prompts default to the current
     * versions in {@link JudgePromptLibrary}; override any of them per metric.
     */
    public static final class Builder {

        private String model;
        private String host = DEFAULT_HOST;
        private int port = DEFAULT_PORT;
        private final Map<MetricType, JudgePromptTemplate> prompts = new EnumMap<>(MetricType.class);

        private Builder() {
        }

        /**
         * Sets the Ollama model name (required).
         *
         * @param modelName the Ollama model name, e.g. {@code "qwen2.5:14b"}
         * @return this, for chaining
         */
        public Builder model(String modelName) {
            this.model = Objects.requireNonNull(modelName, "model");
            return this;
        }

        /**
         * Sets the Ollama server host (defaults to {@code localhost}).
         *
         * @param hostName the Ollama server hostname or IP
         * @return this, for chaining
         */
        public Builder host(String hostName) {
            this.host = Objects.requireNonNull(hostName, "host");
            return this;
        }

        /**
         * Sets the Ollama server port (defaults to {@code 11434}).
         *
         * @param portNumber the Ollama server port
         * @return this, for chaining
         */
        public Builder port(int portNumber) {
            this.port = portNumber;
            return this;
        }

        /**
         * Overrides the prompt template for any metric type.
         *
         * @param type     the metric whose prompt to replace
         * @param template the custom template, replacing the {@link JudgePromptLibrary} default
         * @return this, for chaining
         */
        public Builder prompt(MetricType type, JudgePromptTemplate template) {
            prompts.put(Objects.requireNonNull(type, "type"),
                    Objects.requireNonNull(template, "template"));
            return this;
        }

        /**
         * Overrides the Faithfulness prompt (metric {@link MetricType#GENERATION}).
         *
         * @param template the custom template
         * @return this, for chaining
         */
        public Builder faithfulnessPrompt(JudgePromptTemplate template) {
            return prompt(MetricType.GENERATION, template);
        }

        /**
         * Overrides the AnswerRelevancy prompt.
         *
         * @param template the custom template
         * @return this, for chaining
         */
        public Builder answerRelevancyPrompt(JudgePromptTemplate template) {
            return prompt(MetricType.ANSWER_RELEVANCY, template);
        }

        /**
         * Overrides the FactualCorrectness prompt.
         *
         * @param template the custom template
         * @return this, for chaining
         */
        public Builder factualCorrectnessPrompt(JudgePromptTemplate template) {
            return prompt(MetricType.FACTUAL_CORRECTNESS, template);
        }

        /**
         * Builds the judge.
         *
         * @return a configured {@link OllamaJudge}
         * @throws NullPointerException if no model was set
         */
        public OllamaJudge build() {
            return new OllamaJudge(Objects.requireNonNull(model, "model is required"),
                    host, port, prompts);
        }
    }
}
