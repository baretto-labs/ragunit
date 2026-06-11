package org.ragunit.core.testset;

import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.QuestionType;
import org.ragunit.core.domain.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Generates {@link QuestionType#SIMPLE} test cases: one question per document,
 * answerable from a single source chunk.
 *
 * <p>Prompt instructs the LLM to generate a focused question that can be answered
 * solely from the given document's content.
 */
public final class SimpleQuestionSynthesizer implements QuestionSynthesizer {

    private final OllamaTestsetClient client;

    /**
     * Creates a synthesizer connected to the given Ollama endpoint.
     *
     * @param model the Ollama model name
     * @param host  the Ollama server hostname
     * @param port  the Ollama server port
     */
    public SimpleQuestionSynthesizer(String model, String host, int port) {
        this.client = new OllamaTestsetClient(
                Objects.requireNonNull(model, "model"), host, port);
    }

    @Override
    public List<TestCase> synthesize(List<Document> corpus, int count) {
        Objects.requireNonNull(corpus, "corpus");
        int actual = Math.min(count, corpus.size());
        List<TestCase> cases = new ArrayList<>(actual);
        for (int i = 0; i < actual; i++) {
            Document doc = corpus.get(i);
            String prompt = buildPrompt(doc);
            cases.add(client.callAndParse(prompt, List.of(doc), QuestionType.SIMPLE));
        }
        return cases;
    }

    private static String buildPrompt(Document document) {
        return "You are a RAG evaluation expert.\n\n"
                + "Given the following document, generate ONE focused question that can be "
                + "answered solely from this document's content, and write a concise reference answer.\n\n"
                + "Document:\n" + document.content() + "\n\n"
                + "Reply ONLY with valid JSON — no markdown, no code block:\n"
                + "{\"question\": \"<question>\", \"referenceAnswer\": \"<answer>\"}";
    }
}
