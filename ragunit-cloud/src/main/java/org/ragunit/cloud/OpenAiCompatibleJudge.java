package org.ragunit.cloud;

import org.ragunit.core.judge.HttpJudge;
import org.ragunit.core.judge.JudgePromptLibrary;
import org.ragunit.core.judge.JudgePromptTemplate;
import org.ragunit.core.judge.MetricType;
import org.ragunit.core.judge.RagJudge;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link RagJudge} that calls any <b>OpenAI-compatible</b> Chat Completions API
 * ({@code POST {baseUrl}/chat/completions}).
 *
 * <p>One judge, many providers — point it at a base URL and a model:
 * <ul>
 *   <li><b>OpenAI</b> — {@code https://api.openai.com/v1}</li>
 *   <li><b>Azure OpenAI</b>, <b>Groq</b>, <b>Together</b>, <b>OpenRouter</b>,
 *       <b>Fireworks</b>, <b>DeepInfra</b>…</li>
 *   <li><b>Anthropic Claude</b> — {@code https://api.anthropic.com/v1} (OpenAI-compatible endpoint)</li>
 *   <li><b>Google Gemini</b> — {@code https://generativelanguage.googleapis.com/v1beta/openai}</li>
 *   <li><b>Local servers</b> — vLLM, LM Studio, llama.cpp ({@code http://localhost:.../v1}), usually no key</li>
 * </ul>
 *
 * <p>For a fully local, private setup, prefer {@code OllamaJudge} in {@code ragunit-core}.
 * This judge is the opt-in path when you want a hosted frontier model as the judge or
 * already run an OpenAI-compatible gateway.
 *
 * <p>The API key, when set, is sent as {@code Authorization: Bearer <key>} and is never
 * logged. Pass it from an environment variable rather than hard-coding it:
 * <pre>{@code
 * OpenAiCompatibleJudge judge = OpenAiCompatibleJudge.builder()
 *         .baseUrl("https://api.openai.com/v1")
 *         .apiKey(System.getenv("OPENAI_API_KEY"))
 *         .model("gpt-4o-mini")
 *         .build();
 * }</pre>
 *
 * <p>All prompt, variance, and result machinery is inherited from {@link HttpJudge};
 * this class supplies only the OpenAI wire format.
 */
public final class OpenAiCompatibleJudge extends HttpJudge {

    private final String chatCompletionsUrl;
    private final Optional<String> apiKey;

    private OpenAiCompatibleJudge(Builder builder) {
        super(Objects.requireNonNull(builder.model, "model is required"),
                builder.temperature, builder.prompts);
        String baseUrl = Objects.requireNonNull(builder.baseUrl, "baseUrl is required");
        this.chatCompletionsUrl = stripTrailingSlash(baseUrl) + "/chat/completions";
        this.apiKey = Optional.ofNullable(builder.apiKey);
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /**
     * Starts a fluent builder.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected String endpointUrl() {
        return chatCompletionsUrl;
    }

    @Override
    protected String buildRequestBody(String prompt) {
        return """
                {"model":"%s","messages":[{"role":"user","content":"%s"}],"temperature":%s}\
                """.formatted(escapeJson(model()), escapeJson(prompt), temperature());
    }

    @Override
    protected String[] authorizationHeaders() {
        return apiKey
                .map(key -> new String[] {"Authorization", "Bearer " + key})
                .orElseGet(() -> new String[0]);
    }

    @Override
    protected String extractContent(String responseBody) {
        // OpenAI replies: {"choices":[{"message":{"role":"...","content":"<content>",...}}]}
        return extractJsonString(responseBody, "content");
    }

    /**
     * Fluent builder for {@link OpenAiCompatibleJudge}.
     *
     * <p>{@link #baseUrl(String)} and {@link #model(String)} are required;
     * {@link #apiKey(String)} is optional (omit for keyless local servers).
     */
    public static final class Builder {

        private String baseUrl;
        private String model;
        private String apiKey;
        private double temperature = DEFAULT_TEMPERATURE;
        private final Map<MetricType, JudgePromptTemplate> prompts = new EnumMap<>(MetricType.class);

        private Builder() {
        }

        /**
         * Sets the API base URL up to and including the version segment,
         * e.g. {@code https://api.openai.com/v1} (required). A trailing slash is tolerated.
         *
         * @param url the OpenAI-compatible base URL
         * @return this, for chaining
         */
        public Builder baseUrl(String url) {
            this.baseUrl = Objects.requireNonNull(url, "baseUrl");
            return this;
        }

        /**
         * Sets the model name, e.g. {@code gpt-4o-mini} (required).
         *
         * @param modelName the model identifier the provider expects
         * @return this, for chaining
         */
        public Builder model(String modelName) {
            this.model = Objects.requireNonNull(modelName, "model");
            return this;
        }

        /**
         * Sets the API key, sent as {@code Authorization: Bearer <key>} (optional).
         * Omit for keyless local servers. Prefer reading it from the environment.
         *
         * @param key the API key, or {@code null} for no authorization header
         * @return this, for chaining
         */
        public Builder apiKey(String key) {
            this.apiKey = key;
            return this;
        }

        /**
         * Sets the sampling temperature (defaults to {@code 0.0}, recommended for a judge).
         *
         * @param samplingTemperature the temperature sent to the provider
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
         * Builds the judge.
         *
         * @return a configured {@link OpenAiCompatibleJudge}
         * @throws NullPointerException if {@code baseUrl} or {@code model} is missing
         */
        public OpenAiCompatibleJudge build() {
            return new OpenAiCompatibleJudge(this);
        }
    }
}
