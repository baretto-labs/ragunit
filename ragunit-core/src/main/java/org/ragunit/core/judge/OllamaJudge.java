package org.ragunit.core.judge;

import java.util.EnumMap;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;

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
    private final OptionalInt seed;

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
        this(builder().model(model).host(host).port(port).prompts(templates));
    }

    private OllamaJudge(Builder builder) {
        super(builder.model, builder.temperature, builder.prompts, builder.timeout);
        this.baseUrl = baseUrlOf(builder.host, builder.port);
        this.seed = builder.seed;
    }

    private static String baseUrlOf(String host, int port) {
        return "http://" + Objects.requireNonNull(host, "host") + ":" + port;
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
                "stream":false,"options":{"temperature":%s%s}}\
                """.formatted(escapeJson(model()), escapeJson(prompt), temperature(), seedOption());
    }

    /** Renders the seed as an extra JSON member, or nothing when no seed was configured. */
    private String seedOption() {
        return seed.isPresent() ? ",\"seed\":" + seed.getAsInt() : "";
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
        private Duration timeout = DEFAULT_TIMEOUT;
        private OptionalInt seed = OptionalInt.empty();
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
         * Fixes the sampling seed (no seed by default, so Ollama picks one at random).
         *
         * <p>What it removes is the sampling randomness. Measured on {@code qwen2.5:14b},
         * two unseeded runs of one retrieval query at temperature {@code 0.8} scored
         * {@code 1.0} then {@code 0.95}, while the same pair with a fixed seed returned an
         * identical verdict. At temperature {@code 0.0} both pairs were already identical,
         * so a seed changes nothing observable there.
         *
         * <p>It is not a determinism guarantee: GPU scheduling and serving-side batching
         * still move a score, which is why {@code withRuns(n)} remains useful. And a seed
         * says nothing across a model upgrade or a prompt change — either invalidates a
         * baseline just as a new seed would.
         *
         * @param samplingSeed the Ollama sampling seed; any {@code int}, including zero
         * @return this, for chaining
         */
        public Builder seed(int samplingSeed) {
            this.seed = OptionalInt.of(samplingSeed);
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

        /** Adds a whole template map at once — the path taken by the public constructors. */
        Builder prompts(Map<MetricType, JudgePromptTemplate> templates) {
            prompts.putAll(Objects.requireNonNull(templates, "templates"));
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
         * Sets how long one judge call may take before it is abandoned
         * (defaults to {@link HttpJudge#DEFAULT_TIMEOUT}).
         *
         * <p>Raise it when judging with a large local model: a 14B model answering a
         * faithfulness query on a laptop routinely needs more than the default minute,
         * and every call then fails as a timeout rather than returning a low score.
         *
         * @param requestTimeout the per-call timeout; must be strictly positive
         * @return this, for chaining
         * @throws IllegalArgumentException if the timeout is zero or negative
         */
        public Builder timeout(Duration requestTimeout) {
            this.timeout = HttpJudge.requirePositive(requestTimeout);
            return this;
        }

        /**
         * Builds the judge.
         *
         * @return a configured {@link OllamaJudge}
         * @throws NullPointerException if no model was set
         */
        public OllamaJudge build() {
            Objects.requireNonNull(model, "model is required");
            return new OllamaJudge(this);
        }
    }
}
