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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads a {@link Testset} from a JSON file produced by {@link JsonTestsetExporter}.
 *
 * <p>Hand-written parsing — no external JSON library required.
 * The parser handles escaped characters ({@code \"}, {@code \\}, {@code \n}).
 */
public final class JsonTestsetImporter implements TestsetImporter {

    /** Creates a new JsonTestsetImporter. */
    public JsonTestsetImporter() {
    }

    private static final Pattern QUESTION_PATTERN =
            Pattern.compile("\"question\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern REFERENCE_PATTERN =
            Pattern.compile("\"referenceAnswer\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern CONTEXT_ARRAY_PATTERN =
            Pattern.compile("\"context\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL);
    private static final Pattern STRING_IN_ARRAY_PATTERN =
            Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");

    @Override
    public Testset load(Path source) throws IOException {
        String content = Files.readString(source, StandardCharsets.UTF_8).trim();
        if (content.equals("[]")) {
            return new Testset(List.of());
        }
        return new Testset(extractObjects(content));
    }

    private static List<TestCase> extractObjects(String content) {
        List<TestCase> cases = new ArrayList<>();
        int depth = 0;
        int start = -1;
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (ch == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0 && start != -1) {
                    cases.add(parseObject(content.substring(start, i + 1)));
                    start = -1;
                }
            }
        }
        return cases;
    }

    private static TestCase parseObject(String obj) {
        String question = extractString(QUESTION_PATTERN, obj);
        String reference = extractString(REFERENCE_PATTERN, obj);
        List<Document> context = extractContext(obj);
        return TestCase.simple(new Question(question), context, new ReferenceAnswer(reference));
    }

    private static String extractString(Pattern pattern, String obj) {
        Matcher m = pattern.matcher(obj);
        return m.find() ? unescape(m.group(1)) : "";
    }

    private static List<Document> extractContext(String obj) {
        Matcher arrayMatcher = CONTEXT_ARRAY_PATTERN.matcher(obj);
        if (!arrayMatcher.find()) {
            return List.of();
        }
        String arrayContent = arrayMatcher.group(1);
        List<Document> docs = new ArrayList<>();
        Matcher strMatcher = STRING_IN_ARRAY_PATTERN.matcher(arrayContent);
        while (strMatcher.find()) {
            docs.add(new Document(unescape(strMatcher.group(1))));
        }
        return docs;
    }

    private static String unescape(String value) {
        return value
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }
}
