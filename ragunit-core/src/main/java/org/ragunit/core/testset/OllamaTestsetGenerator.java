package org.ragunit.core.testset;

import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.QuestionType;
import org.ragunit.core.domain.TestCase;
import org.ragunit.core.domain.Testset;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@link TestsetGenerator} implementation that uses a local Ollama model to synthesize
 * question/reference-answer pairs from a document corpus.
 *
 * <p>By default, all generated test cases are {@link QuestionType#SIMPLE}.
 * A distribution map can be provided to mix simple and multi-hop questions:
 * <pre>{@code
 * new OllamaTestsetGenerator("qwen2.5:14b",
 *     Map.of(QuestionType.SIMPLE, 0.7, QuestionType.MULTI_HOP, 0.3))
 * }</pre>
 *
 * <p>If the LLM response cannot be parsed, a fallback {@link TestCase} is produced so the
 * generator never throws on malformed output — only on network or server failures.
 */
public final class OllamaTestsetGenerator implements TestsetGenerator {

    static final String DEFAULT_HOST = "localhost";
    static final int DEFAULT_PORT = 11434;

    private static final Map<QuestionType, Double> DEFAULT_DISTRIBUTION =
            Map.of(QuestionType.SIMPLE, 1.0);

    private final SimpleQuestionSynthesizer simpleSynthesizer;
    private final Map<QuestionType, Double> distribution;
    private final boolean hasMultiHop;
    private MultiHopQuestionSynthesizer multiHopSynthesizer;

    /**
     * Creates a generator using {@code localhost:11434} and all-simple distribution.
     *
     * @param model the Ollama model name (e.g. {@code qwen2.5:14b})
     */
    public OllamaTestsetGenerator(String model) {
        this(model, DEFAULT_HOST, DEFAULT_PORT, DEFAULT_DISTRIBUTION);
    }

    /**
     * Creates a generator with a custom host/port and all-simple distribution.
     *
     * @param model the Ollama model name
     * @param host  the Ollama server hostname
     * @param port  the Ollama server port
     */
    public OllamaTestsetGenerator(String model, String host, int port) {
        this(model, host, port, DEFAULT_DISTRIBUTION);
    }

    /**
     * Creates a generator with a custom distribution of question types.
     *
     * @param model        the Ollama model name
     * @param host         the Ollama server hostname
     * @param port         the Ollama server port
     * @param distribution map of {@link QuestionType} to proportion (values are relative weights)
     */
    public OllamaTestsetGenerator(String model, String host, int port,
                                   Map<QuestionType, Double> distribution) {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(distribution, "distribution");
        this.simpleSynthesizer = new SimpleQuestionSynthesizer(model, host, port);
        this.distribution = Map.copyOf(distribution);
        this.hasMultiHop = distribution.containsKey(QuestionType.MULTI_HOP)
                && distribution.get(QuestionType.MULTI_HOP) > 0.0;
        if (hasMultiHop) {
            this.multiHopSynthesizer = new MultiHopQuestionSynthesizer(model, host, port);
        }
    }

    @Override
    public Testset generate(List<Document> corpus, int count) {
        Objects.requireNonNull(corpus, "corpus");
        if (corpus.isEmpty()) {
            throw new IllegalArgumentException("corpus must not be empty");
        }
        if (count == 0) {
            return new Testset(List.of());
        }
        if (!hasMultiHop) {
            int actual = Math.min(count, corpus.size());
            return new Testset(simpleSynthesizer.synthesize(corpus, actual));
        }
        return generateMixed(corpus, count);
    }

    private Testset generateMixed(List<Document> corpus, int count) {
        double totalWeight = distribution.values().stream().mapToDouble(Double::doubleValue).sum();
        double multiHopRatio = distribution.getOrDefault(QuestionType.MULTI_HOP, 0.0) / totalWeight;
        int multiHopCount = Math.max(1, (int) Math.round(count * multiHopRatio));
        int simpleCount = Math.max(1, count - multiHopCount);

        List<TestCase> cases = new ArrayList<>(count);
        // Simple cases use individual docs; multi-hop needs corpus of >= 2
        int actualSimple = Math.min(simpleCount, corpus.size());
        cases.addAll(simpleSynthesizer.synthesize(corpus.subList(0, actualSimple), actualSimple));
        if (corpus.size() >= 2) {
            cases.addAll(multiHopSynthesizer.synthesize(corpus, multiHopCount));
        }
        return new Testset(cases);
    }
}
