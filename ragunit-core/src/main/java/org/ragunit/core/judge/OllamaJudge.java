package org.ragunit.core.judge;

import java.util.EnumMap;
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
 * <p>Local and private by design: no data leaves the host running Ollama.
 * The provider-independent machinery (prompts, parsing, query dispatch, HTTP) lives
 * in {@link HttpJudge}.
 */
public final class OllamaJudge extends HttpJudge {

    /** Default host when none is specified. */
    static final String DEFAULT_HOST = "localhost";

    /** Default Ollama port when none is specified. */
    static final int DEFAULT_PORT = 11434;

    private final String baseUrl;

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
        this(model, host, port, templates, DEFAULT_TEMPERATURE);
    }

    private OllamaJudge(String model, String host, int port,
                        Map<MetricType, JudgePromptTemplate> templates, double temperature) {
        super(model, temperature, templates);
        this.baseUrl = "http://" + Objects.requireNonNull(host, "host") + ":" + port;
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

    @Override
    protected String endpointUrl() {
        return baseUrl + "/api/chat";
    }

    @Override
    protected String buildRequestBody(String prompt) {
        return """
                {"model":"%s","messages":[{"role":"user","content":"%s"}],\
                "stream":false,"options":{"temperature":%s}}\
                """.formatted(escapeJson(model()), escapeJson(prompt), temperature());
    }

    @Override
    protected String[] authorizationHeaders() {
        return new String[0];
    }

    @Override
    protected String extractContent(String responseBody) {
        // Ollama replies: {"message":{"role":"...","content":"<content>"}}
        return extractJsonString(responseBody, "content");
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
        private double temperature = DEFAULT_TEMPERATURE;
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
         * Sets the sampling temperature (defaults to {@code 0.0}).
         *
         * <p>Zero is the recommended default for an LLM-as-judge: it minimizes
         * run-to-run variance. Raise it only to probe judge stability
         * (e.g. together with {@code withRuns(n)} on the assertion builders).
         *
         * @param samplingTemperature the Ollama sampling temperature
         * @return this, for chaining
         */
        public Builder temperature(double samplingTemperature) {
            this.temperature = samplingTemperature;
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
                    host, port, prompts, temperature);
        }
    }
}
