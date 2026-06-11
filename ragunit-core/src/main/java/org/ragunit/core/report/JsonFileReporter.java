package org.ragunit.core.report;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Objects;

/**
 * Default {@link RagReporter} implementation: silently appends assertion results
 * as JSON entries to {@code target/ragunit-report.json}.
 *
 * <p>The file is a valid JSON array. Entries are appended without rewriting
 * the whole file. Any I/O failure is swallowed — reporters must never interrupt tests.
 *
 * <p>A custom output path can be injected via the constructor (useful for testing).
 */
public final class JsonFileReporter implements RagReporter {

    /** Default output path, relative to the working directory (Maven target/). */
    static final Path DEFAULT_REPORT_PATH = Paths.get("target", "ragunit-report.json");

    private final Path reportPath;

    /** Creates a reporter writing to {@code target/ragunit-report.json}. */
    public JsonFileReporter() {
        this(DEFAULT_REPORT_PATH);
    }

    /**
     * Creates a reporter writing to the given path.
     *
     * @param reportPath the output file path
     */
    public JsonFileReporter(Path reportPath) {
        this.reportPath = Objects.requireNonNull(reportPath, "reportPath");
    }

    @Override
    public void report(AssertionResult result) {
        try {
            appendEntry(buildJsonEntry(result));
        } catch (IOException ignored) {
            // Silent: I/O failures must not interrupt test execution
        }
    }

    private void appendEntry(String jsonEntry) throws IOException {
        Path parent = reportPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (!Files.exists(reportPath)) {
            Files.writeString(reportPath, "[\n" + jsonEntry + "\n]", StandardCharsets.UTF_8);
            return;
        }
        try (RandomAccessFile file = new RandomAccessFile(reportPath.toFile(), "rw")) {
            file.seek(file.length() - 1);
            file.write(",\n".getBytes(StandardCharsets.UTF_8));
            file.write(jsonEntry.getBytes(StandardCharsets.UTF_8));
            file.write("\n]".getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String buildJsonEntry(AssertionResult result) {
        return "{"
                + "\"timestamp\":\"" + Instant.now() + "\","
                + "\"assertionType\":\"" + result.assertionType() + "\","
                + "\"question\":\"" + escapeJson(result.question().text()) + "\","
                + "\"score\":" + result.verdict().score().value() + ","
                + "\"threshold\":" + result.threshold() + ","
                + "\"passed\":" + result.passed() + ","
                + "\"model\":\"" + escapeJson(result.verdict().model()) + "\","
                + "\"rationale\":\"" + escapeJson(result.verdict().rationale()) + "\","
                + "\"statements\":" + buildStatementsJson(result)
                + "}";
    }

    private static String buildStatementsJson(AssertionResult result) {
        var statements = result.verdict().statements();
        if (statements.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < statements.size(); i++) {
            var s = statements.get(i);
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{\"text\":\"").append(escapeJson(s.text())).append("\",")
              .append("\"supported\":").append(s.supported()).append("}");
        }
        return sb.append("]").toString();
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
