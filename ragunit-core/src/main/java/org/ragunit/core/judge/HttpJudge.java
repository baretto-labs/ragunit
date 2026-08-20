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
 * Base class for {@link RagJudge} implementations that call an HTTP chat endpoint.
 *
 * <p>It owns everything that is provider-independent: the {@link JudgePromptLibrary}
 * templates (with per-metric overrides), prompt rendering, the thirteen typed
 * evaluation methods, generic {@link JudgeQuery} dispatch, the HTTP call with a
 * response-size guard, and {@link VerdictParser} parsing with the prompt/response
 * exchange attached to every {@link Verdict}.
 *
 * <p>A concrete provider only supplies the four wire details:
 * {@link #endpointUrl()}, {@link #buildRequestBody(String)},
 * {@link #authorizationHeaders()}, and {@link #extractContent(String)}.
 * {@code OllamaJudge} (local) and {@code OpenAiCompatibleJudge} (any OpenAI-compatible
 * API) are the built-in implementations; a new provider is roughly thirty lines.
 *
 * <p>Uses the JDK built-in {@link HttpClient} — zero extra dependencies. Any network
 * or parse failure is wrapped in a {@link JudgeException}.
 */
public abstract class HttpJudge implements RagJudge {

    /**
     * Request timeout applied when none is configured. LLM inference is slow, and a local
     * model is slower still: a 14B judge answering on a laptop can need more than a minute,
     * which is why {@link #timeout()} is configurable per judge.
     */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    /** Maximum accepted response body size (1 MB). Guards against runaway responses. */
    private static final int MAX_RESPONSE_BYTES = 1024 * 1024;

    /**
     * Default sampling temperature. Zero is the recommended setting for an
     * LLM-as-judge: it minimizes run-to-run variance, making scores as
     * reproducible as the model allows.
     */
    public static final double DEFAULT_TEMPERATURE = 0.0;

    private final String model;
    private final double temperature;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final Map<MetricType, JudgePromptTemplate> templates;

    /**
     * Creates a judge with the given model, sampling temperature, and prompt overrides.
     *
     * @param model       the model identifier embedded in every verdict
     * @param temperature the sampling temperature passed to the provider
     * @param templates   per-metric prompt overrides; missing keys fall back to
     *                    {@link JudgePromptLibrary} defaults
     */
    protected HttpJudge(String model, double temperature,
                        Map<MetricType, JudgePromptTemplate> templates) {
        this(model, temperature, templates, DEFAULT_TIMEOUT);
    }

    /**
     * Creates a judge with an explicit request timeout.
     *
     * @param model       the model identifier embedded in every verdict
     * @param temperature the sampling temperature passed to the provider
     * @param templates   per-metric prompt overrides; missing keys fall back to
     *                    {@link JudgePromptLibrary} defaults
     * @param timeout     how long one judge call may take before it is abandoned; the builders
     *                    validate it with {@link #requirePositive(Duration)} before calling this
     */
    protected HttpJudge(String model, double temperature,
                        Map<MetricType, JudgePromptTemplate> templates, Duration timeout) {
        this.model = Objects.requireNonNull(model, "model");
        this.temperature = temperature;
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        this.httpClient = HttpClient.newHttpClient();
        this.templates = mergeWithDefaults(Objects.requireNonNull(templates, "templates"));
    }

    /**
     * Validates a request timeout.
     *
     * @param timeout the candidate timeout
     * @return the same timeout when it is strictly positive
     * @throws IllegalArgumentException if it is zero or negative
     */
    public static Duration requirePositive(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be strictly positive, was " + timeout);
        }
        return timeout;
    }

    private static Map<MetricType, JudgePromptTemplate> mergeWithDefaults(
            Map<MetricType, JudgePromptTemplate> overrides) {
        Map<MetricType, JudgePromptTemplate> merged = new EnumMap<>(JudgePromptLibrary.defaults());
        merged.putAll(overrides);
        return Map.copyOf(merged);
    }

    /**
     * How long one judge call may take before it is abandoned.
     *
     * @return the configured request timeout
     */
    protected final Duration timeout() {
        return timeout;
    }

    /**
     * The model identifier this judge sends and embeds in verdicts.
     *
     * @return the model name
     */
    protected final String model() {
        return model;
    }

    /**
     * The configured sampling temperature.
     *
     * @return the temperature
     */
    protected final double temperature() {
        return temperature;
    }

    // --- provider-specific wire details ---

    /**
     * The full chat-completion URL to POST to.
     *
     * @return the endpoint URL
     */
    protected abstract String endpointUrl();

    /**
     * Builds the provider-specific JSON request body for a single user prompt.
     * Use {@link #escapeJson(String)} for string values.
     *
     * @param prompt the rendered judge prompt
     * @return the JSON request body
     */
    protected abstract String buildRequestBody(String prompt);

    /**
     * Provider authorization headers (e.g. {@code Authorization: Bearer ...}),
     * as alternating header-name/value pairs. Empty for unauthenticated endpoints.
     *
     * @return alternating name/value header pairs, never null
     */
    protected abstract String[] authorizationHeaders();

    /**
     * Extracts the model's textual reply from the provider's JSON response body.
     *
     * @param responseBody the raw HTTP response body
     * @return the model's reply text (the structured JSON the judge was asked for)
     * @throws JudgeException if the reply cannot be located
     */
    protected abstract String extractContent(String responseBody);

    // --- typed evaluations (provider-independent) ---

    @Override
    public Verdict evaluateRetrieval(Question question, List<Document> context) {
        return call(render(MetricType.RETRIEVAL, PromptContext.forRetrieval(question, context)));
    }

    @Override
    public Verdict evaluateGeneration(Question question, List<Document> context, Answer answer) {
        return call(render(MetricType.GENERATION,
                PromptContext.forGeneration(question, context, answer)));
    }

    @Override
    public Verdict evaluateContextRejection(Question question, List<Document> context) {
        return call(render(MetricType.CONTEXT_REJECTION,
                PromptContext.forRetrieval(question, context)));
    }

    @Override
    public Verdict evaluateRejection(Question question, List<Document> context, Answer answer) {
        return call(render(MetricType.REJECTION,
                PromptContext.forGeneration(question, context, answer)));
    }

    @Override
    public Verdict evaluateContextRecall(Question question, List<Document> context, ReferenceAnswer reference) {
        return call(render(MetricType.CONTEXT_RECALL,
                PromptContext.forContextRecall(question, context, reference)));
    }

    @Override
    public Verdict evaluateAnswerRelevancy(Question question, Answer answer) {
        return call(render(MetricType.ANSWER_RELEVANCY,
                PromptContext.forGeneration(question, List.of(), answer)));
    }

    @Override
    public Verdict evaluateContextPrecision(Question question, List<Document> context) {
        String prompt = render(MetricType.CONTEXT_PRECISION, PromptContext.forRetrieval(question, context));
        String response = rawCall(prompt);
        return VerdictParser.parse(response, model, context).withExchange(prompt, response);
    }

    @Override
    public FactualCorrectnessVerdict evaluateFactualCorrectness(
            Question question, Answer answer, ReferenceAnswer reference) {
        String prompt = render(MetricType.FACTUAL_CORRECTNESS,
                PromptContext.forFactualCorrectness(question, answer, reference));
        return VerdictParser.parseFactualCorrectness(rawCall(prompt), model);
    }

    @Override
    public Verdict evaluateToolTrajectory(Question question, List<ToolCall> trajectory, Answer answer) {
        return call(render(MetricType.TOOL_TRAJECTORY,
                PromptContext.forToolTrajectory(question, trajectory, answer)));
    }

    @Override
    public Verdict evaluateContextPromptInjection(Question question, List<Document> context) {
        return call(render(MetricType.CONTEXT_PROMPT_INJECTION,
                PromptContext.forRetrieval(question, context)));
    }

    @Override
    public Verdict evaluatePromptInjection(Question question, List<Document> context, Answer answer) {
        return call(render(MetricType.PROMPT_INJECTION,
                PromptContext.forGeneration(question, context, answer)));
    }

    @Override
    public Verdict evaluateContextPIILeak(Question question, List<Document> context) {
        return call(render(MetricType.CONTEXT_PII_LEAK,
                PromptContext.forRetrieval(question, context)));
    }

    @Override
    public Verdict evaluatePIILeak(Question question, List<Document> context, Answer answer) {
        return call(render(MetricType.PII_LEAK,
                PromptContext.forGeneration(question, context, answer)));
    }

    /**
     * Evaluates a generic query. Built-in {@link MetricType} criteria use the
     * configured per-metric prompt templates; any other {@link Criterion} is judged
     * with the generic {@link JudgePromptLibrary#criterionPromptV1(JudgeQuery)} prompt.
     *
     * @param query the criterion and named inputs to judge
     * @return the full verdict, carrying the exact prompt and raw response
     */
    @Override
    public Verdict verdictFor(JudgeQuery query) {
        if (query.criterion() instanceof MetricType) {
            return RagJudge.super.verdictFor(query);
        }
        return call(JudgePromptLibrary.criterionPromptV1(query));
    }

    private String render(MetricType type, PromptContext ctx) {
        return templates.get(type).render(ctx);
    }

    private Verdict call(String prompt) {
        String response = rawCall(prompt);
        return VerdictParser.parse(response, model).withExchange(prompt, response);
    }

    private String rawCall(String prompt) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl()))
                    .header("Content-Type", "application/json")
                    .timeout(timeout)
                    .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(prompt)));
            String[] headers = authorizationHeaders();
            for (int i = 0; i + 1 < headers.length; i += 2) {
                builder.header(headers[i], headers[i + 1]);
            }
            HttpResponse<String> response = httpClient.send(builder.build(), limitedBodyHandler());
            return extractContent(response.body());
        } catch (IOException e) {
            throw new JudgeException("Judge HTTP call failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JudgeException("Judge HTTP call interrupted", e);
        }
    }

    private static HttpResponse.BodyHandler<String> limitedBodyHandler() {
        return info -> HttpResponse.BodySubscribers.mapping(
                HttpResponse.BodySubscribers.ofByteArray(),
                bytes -> {
                    if (bytes.length > MAX_RESPONSE_BYTES) {
                        throw new JudgeException(
                                "Judge response exceeds size limit (%d bytes)".formatted(MAX_RESPONSE_BYTES));
                    }
                    return new String(bytes, StandardCharsets.UTF_8);
                });
    }

    /**
     * JSON-escapes a string value for inclusion in a request body.
     *
     * @param value the raw string
     * @return the escaped string (without surrounding quotes)
     */
    protected static String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Extracts the first JSON string value for the given key, honoring backslash
     * escapes. Shared by providers whose reply text lives under a {@code content} key.
     *
     * @param body the JSON body to scan
     * @param key  the string key whose value to read (e.g. {@code "content"})
     * @return the unescaped string value
     * @throws JudgeException if the key or a terminating quote is not found
     */
    protected static String extractJsonString(String body, String key) {
        String marker = "\"" + key + "\":\"";
        int start = body.indexOf(marker);
        if (start == -1) {
            throw new JudgeException("Failed to parse judge response: missing '" + key + "' key");
        }
        int valueStart = start + marker.length();
        int valueEnd = closingQuote(body, valueStart);
        if (valueEnd == -1) {
            throw new JudgeException("Failed to parse judge response: unterminated '" + key + "' value");
        }
        return unescapeJson(body.substring(valueStart, valueEnd));
    }

    /** Index of the first unescaped {@code "} at or after {@code from}, or -1. */
    private static int closingQuote(String body, int from) {
        for (int i = from; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    private static String unescapeJson(String value) {
        return value
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
