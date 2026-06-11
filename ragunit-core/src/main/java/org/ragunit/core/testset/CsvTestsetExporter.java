package org.ragunit.core.testset;

import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.TestCase;
import org.ragunit.core.domain.Testset;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * Exports a {@link Testset} to a CSV file.
 *
 * <p>Format: one row per {@link TestCase}, with columns {@code question},
 * {@code referenceAnswer}, and {@code context}. Multiple context documents
 * are joined with the {@code |||} separator. Fields containing commas or
 * double-quotes are quoted and internal double-quotes are doubled ({@code ""}).
 *
 * <p>Zero external dependencies — hand-written RFC 4180-compatible serialization.
 */
public final class CsvTestsetExporter implements TestsetExporter {

    /** Creates a new CsvTestsetExporter. */
    public CsvTestsetExporter() {
    }

    static final String CONTEXT_SEPARATOR = "|||";
    private static final String HEADER = "question,referenceAnswer,context";

    @Override
    public void export(Testset testset, Path destination) throws IOException {
        Path parent = destination.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        StringBuilder sb = new StringBuilder(HEADER).append('\n');
        for (TestCase tc : testset.cases()) {
            sb.append(toCsvRow(tc)).append('\n');
        }
        Files.writeString(destination, sb.toString(), StandardCharsets.UTF_8);
    }

    private static String toCsvRow(TestCase tc) {
        String context = tc.context().stream()
                .map(Document::content)
                .collect(Collectors.joining(CONTEXT_SEPARATOR));
        return csvField(tc.question().text())
                + "," + csvField(tc.referenceAnswer().text())
                + "," + csvField(context);
    }

    static String csvField(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
