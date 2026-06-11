package org.ragunit.core.testset;

import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.QuestionType;
import org.ragunit.core.domain.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Generates {@link QuestionType#MULTI_HOP} test cases: each question requires
 * information from two or more documents to be answered correctly.
 *
 * <p>Pairs of consecutive documents are combined into a single prompt. The LLM is
 * instructed to produce a question that is unanswerable from either document alone.
 *
 * <p>Requires a corpus of at least 2 documents.
 */
public final class MultiHopQuestionSynthesizer implements QuestionSynthesizer {

    private static final int MIN_CORPUS_SIZE = 2;
    private static final int DOCS_PER_HOP = 2;

    private final OllamaTestsetClient client;

    /**
     * Creates a synthesizer connected to the given Ollama endpoint.
     *
     * @param model the Ollama model name
     * @param host  the Ollama server hostname
     * @param port  the Ollama server port
     */
    public MultiHopQuestionSynthesizer(String model, String host, int port) {
        this.client = new OllamaTestsetClient(
                Objects.requireNonNull(model, "model"), host, port);
    }

    @Override
    public List<TestCase> synthesize(List<Document> corpus, int count) {
        Objects.requireNonNull(corpus, "corpus");
        if (corpus.size() < MIN_CORPUS_SIZE) {
            throw new IllegalArgumentException(
                    "Multi-hop synthesis requires at least 2 documents, got: " + corpus.size());
        }
        List<TestCase> cases = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            // Rotate pairs: (0,1), (1,2), (2,0), ...
            int idxA = i % corpus.size();
            int idxB = (i + 1) % corpus.size();
            List<Document> pair = List.of(corpus.get(idxA), corpus.get(idxB));
            String prompt = buildPrompt(pair);
            cases.add(client.callAndParse(prompt, pair, QuestionType.MULTI_HOP));
        }
        return cases;
    }

    private static String buildPrompt(List<Document> docs) {
        StringBuilder prompt = new StringBuilder(
                "You are a RAG evaluation expert.\n\n"
                + "Given the following " + DOCS_PER_HOP + " document fragments, generate ONE question "
                + "that REQUIRES information from BOTH fragments to answer correctly, "
                + "and write a concise reference answer.\n\n");
        for (int i = 0; i < docs.size(); i++) {
            prompt.append("Fragment ").append(i + 1).append(":\n")
                    .append(docs.get(i).content()).append("\n\n");
        }
        prompt.append("Reply ONLY with valid JSON — no markdown, no code block:\n")
                .append("{\"question\": \"<question>\", \"referenceAnswer\": \"<answer>\"}");
        return prompt.toString();
    }
}
