package org.ragunit.core.report;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.Score;
import org.ragunit.core.domain.Statement;
import org.ragunit.core.domain.Verdict;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/** Tests for JsonFileReporter: JSON output format and append behavior. */
class JsonFileReporterTest {

    private static final double SCORE_VALUE = 0.92;
    private static final double THRESHOLD = 0.90;
    private static final String MODEL = "llama3:8b";
    private static final String RATIONALE = "Answer is grounded in context.";
    private static final Question QUESTION = new Question("What is the capital of France?");

    private static AssertionResult passingResult() {
        Verdict verdict = Verdict.of(new Score(SCORE_VALUE), RATIONALE, MODEL);
        return new AssertionResult("FAITHFULNESS", QUESTION, verdict, THRESHOLD, true);
    }

    private static AssertionResult failingResult() {
        Verdict verdict = Verdict.of(new Score(SCORE_VALUE), RATIONALE, MODEL);
        return new AssertionResult("FAITHFULNESS", QUESTION, verdict, THRESHOLD, false);
    }

    @Test
    void should_writeJsonFile_when_assertionCompletes(@TempDir Path tmpDir) throws IOException {
        Path report = tmpDir.resolve("report.json");
        new JsonFileReporter(report).report(passingResult());

        assertThat(report).exists();
        assertThat(Files.readString(report)).startsWith("[").endsWith("]");
    }

    @Test
    void should_appendToExistingFile_when_fileAlreadyExists(@TempDir Path tmpDir) throws IOException {
        Path report = tmpDir.resolve("report.json");
        JsonFileReporter reporter = new JsonFileReporter(report);
        reporter.report(passingResult());
        reporter.report(passingResult());

        String content = Files.readString(report);
        assertThat(content).startsWith("[").endsWith("]");
        // Two entries: two assertion objects at the top level, separated by a comma
        assertThat(content.split("\"assertionType\"").length - 1).isEqualTo(2);
        assertThat(content).contains(",");
    }

    @Test
    void should_createFileInNestedDirectory_when_targetDirectoryDoesNotExist(@TempDir Path tmpDir) throws IOException {
        Path report = tmpDir.resolve("subdir/nested/report.json");
        new JsonFileReporter(report).report(passingResult());

        assertThat(report).exists();
        assertThat(Files.readString(report)).startsWith("[").endsWith("]");
    }

    @Test
    void should_includeTimestamp_when_writing(@TempDir Path tmpDir) throws IOException {
        Path report = tmpDir.resolve("report.json");
        new JsonFileReporter(report).report(passingResult());

        assertThat(Files.readString(report)).contains("\"timestamp\":");
    }

    @Test
    void should_includeScoreAndThreshold_when_writing(@TempDir Path tmpDir) throws IOException {
        Path report = tmpDir.resolve("report.json");
        new JsonFileReporter(report).report(passingResult());

        String content = Files.readString(report);
        assertThat(content).contains("\"score\":" + SCORE_VALUE);
        assertThat(content).contains("\"threshold\":" + THRESHOLD);
    }

    @Test
    void should_markAsFailed_when_assertionFailed(@TempDir Path tmpDir) throws IOException {
        Path report = tmpDir.resolve("report.json");
        new JsonFileReporter(report).report(failingResult());

        assertThat(Files.readString(report)).contains("\"passed\":false");
    }

    @Test
    void should_escapeSpecialChars_when_questionContainsQuotes(@TempDir Path tmpDir) throws IOException {
        Question questionWithQuotes = new Question("What is a \"RAG\" pipeline?");
        Verdict verdict = Verdict.of(new Score(SCORE_VALUE), RATIONALE, MODEL);
        AssertionResult result = new AssertionResult("FAITHFULNESS", questionWithQuotes, verdict, THRESHOLD, true);
        Path report = tmpDir.resolve("report.json");
        new JsonFileReporter(report).report(result);

        String content = Files.readString(report);
        assertThat(content).contains("\\\"RAG\\\"");
        assertThat(content).startsWith("[").endsWith("]");
    }

    @Test
    void should_includeStatements_when_verdictHasStatements(@TempDir Path tmpDir) throws IOException {
        Statement s1 = new Statement("Paris is the capital.", true);
        Statement s2 = new Statement("France is in Asia.", false);
        Verdict verdict = new Verdict(new Score(SCORE_VALUE), RATIONALE, MODEL,
                List.of(s1, s2), List.of());
        AssertionResult result = new AssertionResult("FAITHFULNESS", QUESTION, verdict, THRESHOLD, true);
        Path report = tmpDir.resolve("report.json");
        new JsonFileReporter(report).report(result);

        String content = Files.readString(report);
        assertThat(content).contains("\"Paris is the capital.\"");
        assertThat(content).contains("\"supported\":true");
        assertThat(content).contains("\"France is in Asia.\"");
        assertThat(content).contains("\"supported\":false");
    }

    @Test
    void should_notThrow_when_ioErrorOccurs() {
        // reportPath points to a directory, not a file — write will fail silently
        Path invalidPath = Path.of(System.getProperty("java.io.tmpdir"));
        assertThatNoException().isThrownBy(() -> new JsonFileReporter(invalidPath).report(passingResult()));
    }
}
