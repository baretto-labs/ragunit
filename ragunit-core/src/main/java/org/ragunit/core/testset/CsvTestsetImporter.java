package org.ragunit.core.testset;

import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.ReferenceAnswer;
import org.ragunit.core.domain.TestCase;
import org.ragunit.core.domain.Testset;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Loads a {@link Testset} from a CSV file produced by {@link CsvTestsetExporter}.
 *
 * <p>Handles RFC 4180-compatible quoting: fields quoted with {@code "}, internal
 * double-quotes represented as {@code ""}. Context documents are split on {@code |||}.
 */
public final class CsvTestsetImporter implements TestsetImporter {

    /** Creates a new CsvTestsetImporter. */
    public CsvTestsetImporter() {
    }

    @Override
    public Testset load(Path source) throws IOException {
        List<String> lines = Files.readAllLines(source, StandardCharsets.UTF_8);
        if (lines.size() <= 1) {
            return new Testset(List.of());
        }
        List<TestCase> cases = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.isBlank()) {
                cases.add(parseLine(line));
            }
        }
        return new Testset(cases);
    }

    private static TestCase parseLine(String line) {
        List<String> fields = parseCsvLine(line);
        String questionText = fields.get(0);
        String referenceText = fields.get(1);
        String contextRaw = fields.get(2);
        List<Document> context = Arrays.stream(contextRaw.split("\\|\\|\\|"))
                .filter(s -> !s.isBlank())
                .map(Document::new)
                .toList();
        return TestCase.simple(new Question(questionText), context, new ReferenceAnswer(referenceText));
    }

    static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        int pos = 0;
        while (pos <= line.length()) {
            if (pos < line.length() && line.charAt(pos) == '"') {
                // Quoted field
                StringBuilder sb = new StringBuilder();
                pos++; // skip opening quote
                while (pos < line.length()) {
                    char ch = line.charAt(pos);
                    if (ch == '"') {
                        if (pos + 1 < line.length() && line.charAt(pos + 1) == '"') {
                            sb.append('"');
                            pos += 2;
                        } else {
                            pos++; // skip closing quote
                            break;
                        }
                    } else {
                        sb.append(ch);
                        pos++;
                    }
                }
                fields.add(sb.toString());
                if (pos < line.length() && line.charAt(pos) == ',') {
                    pos++;
                }
            } else {
                // Unquoted field
                int end = line.indexOf(',', pos);
                if (end == -1) {
                    fields.add(line.substring(pos));
                    break;
                }
                fields.add(line.substring(pos, end));
                pos = end + 1;
            }
        }
        return fields;
    }
}
