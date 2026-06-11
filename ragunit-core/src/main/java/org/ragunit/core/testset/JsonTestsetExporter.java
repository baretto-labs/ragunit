package org.ragunit.core.testset;

import org.ragunit.core.domain.TestCase;
import org.ragunit.core.domain.Testset;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Exports a {@link Testset} to a JSON file.
 *
 * <p>Output format: a JSON array where each element is a {@code TestCase} object:
 * <pre>{@code
 * [
 *   {
 *     "question": "What is the capital of France?",
 *     "referenceAnswer": "Paris.",
 *     "context": ["Paris is the capital of France.", "France is in Western Europe."]
 *   }
 * ]
 * }</pre>
 *
 * <p>Zero external dependencies — hand-written JSON serialization.
 */
public final class JsonTestsetExporter implements TestsetExporter {

    /** Creates a new JsonTestsetExporter. */
    public JsonTestsetExporter() {
    }

    @Override
    public void export(Testset testset, Path destination) throws IOException {
        Path parent = destination.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(destination, toJson(testset), StandardCharsets.UTF_8);
    }

    private static String toJson(Testset testset) {
        if (testset.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[\n");
        var cases = testset.cases();
        for (int i = 0; i < cases.size(); i++) {
            sb.append(toCaseJson(cases.get(i)));
            if (i < cases.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        return sb.append("]").toString();
    }

    private static String toCaseJson(TestCase tc) {
        StringBuilder sb = new StringBuilder("  {\n");
        sb.append("    \"question\":\"").append(escapeJson(tc.question().text())).append("\",\n");
        sb.append("    \"referenceAnswer\":\"").append(escapeJson(tc.referenceAnswer().text())).append("\",\n");
        sb.append("    \"context\":[");
        var docs = tc.context();
        for (int i = 0; i < docs.size(); i++) {
            sb.append("\"").append(escapeJson(docs.get(i).content())).append("\"");
            if (i < docs.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]\n  }");
        return sb.toString();
    }

    static String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
