package org.ragunit.core.testset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.ReferenceAnswer;
import org.ragunit.core.domain.TestCase;
import org.ragunit.core.domain.Testset;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests for CSV and JSON export/import round-trips. */
class TestsetExporterTest {

    private static final Question QUESTION = new Question("What is the capital of France?");
    private static final ReferenceAnswer REFERENCE = new ReferenceAnswer("Paris.");
    private static final Document DOC_A = new Document("Paris is the capital of France.");
    private static final Document DOC_B = new Document("France is in Western Europe.");
    private static final TestCase CASE = TestCase.simple(QUESTION, List.of(DOC_A, DOC_B), REFERENCE);
    private static final Testset TESTSET = new Testset(List.of(CASE));

    // --- CsvTestsetExporter ---

    @Test
    void should_writeHeaderAndOneRow_when_exportingToCsv(@TempDir Path tmpDir) throws IOException {
        Path file = tmpDir.resolve("testset.csv");
        new CsvTestsetExporter().export(TESTSET, file);

        List<String> lines = Files.readAllLines(file);
        assertThat(lines.get(0)).isEqualTo("question,referenceAnswer,context");
        assertThat(lines).hasSize(2);
    }

    @Test
    void should_includeAllFields_when_exportingToCsv(@TempDir Path tmpDir) throws IOException {
        Path file = tmpDir.resolve("testset.csv");
        new CsvTestsetExporter().export(TESTSET, file);

        String content = Files.readString(file);
        assertThat(content).contains("What is the capital of France?");
        assertThat(content).contains("Paris.");
        assertThat(content).contains("Paris is the capital of France.");
        assertThat(content).contains("France is in Western Europe.");
    }

    @Test
    void should_separateContextDocsWithPipe_when_multipleDocsInContext(@TempDir Path tmpDir) throws IOException {
        Path file = tmpDir.resolve("testset.csv");
        new CsvTestsetExporter().export(TESTSET, file);

        String content = Files.readString(file);
        assertThat(content).contains("|||");
    }

    @Test
    void should_writeEmptyFile_when_testsetIsEmpty(@TempDir Path tmpDir) throws IOException {
        Path file = tmpDir.resolve("testset.csv");
        new CsvTestsetExporter().export(new Testset(List.of()), file);

        List<String> lines = Files.readAllLines(file);
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0)).isEqualTo("question,referenceAnswer,context");
    }

    @Test
    void should_quoteField_when_questionContainsComma(@TempDir Path tmpDir) throws IOException {
        Question q = new Question("What is bigger, Paris or London?");
        TestCase tc = TestCase.simple(q, List.of(DOC_A), REFERENCE);
        Path file = tmpDir.resolve("testset.csv");
        new CsvTestsetExporter().export(new Testset(List.of(tc)), file);

        String content = Files.readString(file);
        assertThat(content).contains("\"What is bigger, Paris or London?\"");
    }

    @Test
    void should_escapeQuotes_when_fieldContainsDoubleQuote(@TempDir Path tmpDir) throws IOException {
        Question q = new Question("What is a \"RAG\" pipeline?");
        TestCase tc = TestCase.simple(q, List.of(DOC_A), REFERENCE);
        Path file = tmpDir.resolve("testset.csv");
        new CsvTestsetExporter().export(new Testset(List.of(tc)), file);

        String content = Files.readString(file);
        assertThat(content).contains("\"\"RAG\"\"");
    }

    // --- CsvTestsetImporter ---

    @Test
    void should_roundTrip_when_exportThenImportCsv(@TempDir Path tmpDir) throws IOException {
        Path file = tmpDir.resolve("testset.csv");
        new CsvTestsetExporter().export(TESTSET, file);
        Testset loaded = new CsvTestsetImporter().load(file);

        assertThat(loaded.size()).isEqualTo(1);
        assertThat(loaded.cases().get(0).question().text())
                .isEqualTo("What is the capital of France?");
        assertThat(loaded.cases().get(0).referenceAnswer().text()).isEqualTo("Paris.");
        assertThat(loaded.cases().get(0).context()).hasSize(2);
        assertThat(loaded.cases().get(0).context().get(0).content())
                .isEqualTo("Paris is the capital of France.");
    }

    @Test
    void should_roundTripWithComma_when_questionContainsComma(@TempDir Path tmpDir) throws IOException {
        Question q = new Question("What is bigger, Paris or London?");
        Testset original = new Testset(List.of(TestCase.simple(q, List.of(DOC_A), REFERENCE)));
        Path file = tmpDir.resolve("testset.csv");
        new CsvTestsetExporter().export(original, file);
        Testset loaded = new CsvTestsetImporter().load(file);

        assertThat(loaded.cases().get(0).question().text())
                .isEqualTo("What is bigger, Paris or London?");
    }

    @Test
    void should_throwException_when_csvFileDoesNotExist(@TempDir Path tmpDir) {
        Path missing = tmpDir.resolve("missing.csv");
        assertThatThrownBy(() -> new CsvTestsetImporter().load(missing))
                .isInstanceOf(IOException.class);
    }

    // --- JsonTestsetExporter ---

    @Test
    void should_writeValidJsonArray_when_exportingToJson(@TempDir Path tmpDir) throws IOException {
        Path file = tmpDir.resolve("testset.json");
        new JsonTestsetExporter().export(TESTSET, file);

        String content = Files.readString(file);
        assertThat(content).startsWith("[").endsWith("]");
        assertThat(content).contains("\"question\"");
        assertThat(content).contains("\"referenceAnswer\"");
        assertThat(content).contains("\"context\"");
    }

    @Test
    void should_writeEmptyArray_when_testsetIsEmpty(@TempDir Path tmpDir) throws IOException {
        Path file = tmpDir.resolve("testset.json");
        new JsonTestsetExporter().export(new Testset(List.of()), file);

        assertThat(Files.readString(file).trim()).isEqualTo("[]");
    }

    @Test
    void should_includeAllContextDocs_when_exportingToJson(@TempDir Path tmpDir) throws IOException {
        Path file = tmpDir.resolve("testset.json");
        new JsonTestsetExporter().export(TESTSET, file);

        String content = Files.readString(file);
        assertThat(content).contains("Paris is the capital of France.");
        assertThat(content).contains("France is in Western Europe.");
    }

    // --- JsonTestsetImporter ---

    @Test
    void should_roundTrip_when_exportThenImportJson(@TempDir Path tmpDir) throws IOException {
        Path file = tmpDir.resolve("testset.json");
        new JsonTestsetExporter().export(TESTSET, file);
        Testset loaded = new JsonTestsetImporter().load(file);

        assertThat(loaded.size()).isEqualTo(1);
        assertThat(loaded.cases().get(0).question().text())
                .isEqualTo("What is the capital of France?");
        assertThat(loaded.cases().get(0).referenceAnswer().text()).isEqualTo("Paris.");
        assertThat(loaded.cases().get(0).context()).hasSize(2);
    }

    @Test
    void should_roundTripMultipleCases_when_testsetHasManyEntries(@TempDir Path tmpDir) throws IOException {
        TestCase case2 = TestCase.simple(
                new Question("When was the Eiffel Tower built?"),
                List.of(new Document("The Eiffel Tower was built in 1889.")),
                new ReferenceAnswer("1889."));
        Testset multi = new Testset(List.of(CASE, case2));
        Path file = tmpDir.resolve("testset.json");
        new JsonTestsetExporter().export(multi, file);
        Testset loaded = new JsonTestsetImporter().load(file);

        assertThat(loaded.size()).isEqualTo(2);
        assertThat(loaded.cases().get(1).question().text())
                .isEqualTo("When was the Eiffel Tower built?");
    }

    @Test
    void should_throwException_when_jsonFileDoesNotExist(@TempDir Path tmpDir) {
        Path missing = tmpDir.resolve("missing.json");
        assertThatThrownBy(() -> new JsonTestsetImporter().load(missing))
                .isInstanceOf(IOException.class);
    }

    @Test
    void should_escapeSpecialChars_when_questionContainsQuotesInJson(@TempDir Path tmpDir) throws IOException {
        Question q = new Question("What is a \"RAG\" pipeline?");
        Testset original = new Testset(List.of(TestCase.simple(q, List.of(DOC_A), REFERENCE)));
        Path file = tmpDir.resolve("testset.json");
        new JsonTestsetExporter().export(original, file);
        Testset loaded = new JsonTestsetImporter().load(file);

        assertThat(loaded.cases().get(0).question().text())
                .isEqualTo("What is a \"RAG\" pipeline?");
    }
}
