package org.ragunit.core.judge;

import org.ragunit.core.domain.ChunkVerdict;
import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.FactualCorrectnessVerdict;
import org.ragunit.core.domain.Score;
import org.ragunit.core.domain.Statement;
import org.ragunit.core.domain.Verdict;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a structured JSON response from a judge LLM into a {@link Verdict}.
 *
 * <p>Expected JSON format:
 * <pre>{@code
 * {
 *   "score": 0.85,
 *   "rationale": "3 out of 4 claims are supported.",
 *   "statements": [
 *     { "text": "Paris is in France.", "supported": true },
 *     { "text": "Paris has 5 million people.", "supported": false }
 *   ]
 * }
 * }</pre>
 *
 * <p>If the response is not valid JSON or fields are missing, returns a degraded
 * {@link Verdict} with score {@code 0.0}, the raw response as rationale, and no statements.
 * This class never throws — the judge always returns a Verdict.
 */
public final class VerdictParser {

    private static final Pattern SCORE_PATTERN =
            Pattern.compile("\"score\"\\s*:\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern PRECISION_PATTERN =
            Pattern.compile("\"precision\"\\s*:\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern RECALL_PATTERN =
            Pattern.compile("\"recall\"\\s*:\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern RATIONALE_PATTERN =
            Pattern.compile("\"rationale\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern TEXT_PATTERN =
            Pattern.compile("\"text\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern SUPPORTED_PATTERN =
            Pattern.compile("\"supported\"\\s*:\\s*(true|false)");
    private static final Pattern STATEMENTS_START_PATTERN =
            Pattern.compile("\"statements\"\\s*:\\s*\\[");
    private static final Pattern CHUNKS_START_PATTERN =
            Pattern.compile("\"chunks\"\\s*:\\s*\\[");
    private static final Pattern RANK_PATTERN =
            Pattern.compile("\"rank\"\\s*:\\s*(\\d+)");
    private static final Pattern RELEVANT_PATTERN =
            Pattern.compile("\"relevant\"\\s*:\\s*(true|false)");

    private VerdictParser() {}

    /**
     * Parses a judge LLM response string into a {@link Verdict}.
     *
     * @param response  the raw string returned by the LLM
     * @param modelName the model identifier to embed in the Verdict
     * @return a parsed Verdict, or a degraded one if parsing fails
     */
    public static Verdict parse(String response, String modelName) {
        double score = extractScore(response);
        String rationale = extractRationale(response, response.trim());
        List<Statement> statements = extractStatements(response);
        return new Verdict(new Score(score), rationale, modelName, statements, List.of());
    }

    /**
     * Parses a judge LLM response that includes a {@code chunks} array into a {@link Verdict}
     * with per-chunk verdicts linked to the original documents.
     *
     * @param response  the raw string returned by the LLM
     * @param modelName the model identifier to embed in the Verdict
     * @param context   the original document list (rank 1 = index 0)
     * @return a parsed Verdict with chunk verdicts, or a degraded one if parsing fails
     */
    public static Verdict parse(String response, String modelName, List<Document> context) {
        double score = extractScore(response);
        String rationale = extractRationale(response, response.trim());
        List<Statement> statements = extractStatements(response);
        List<ChunkVerdict> chunkVerdicts = extractChunkVerdicts(response, context);
        return new Verdict(new Score(score), rationale, modelName, statements, chunkVerdicts);
    }

    /**
     * Parses a FactualCorrectness judge response into a {@link FactualCorrectnessVerdict}.
     *
     * <p>Expects JSON with {@code score} (F1), {@code precision}, {@code recall},
     * and {@code rationale}. Degrades gracefully: missing fields default to {@code 0.0}.
     *
     * @param response  the raw string returned by the LLM
     * @param modelName the model identifier to embed in the verdict
     * @return a parsed FactualCorrectnessVerdict, never null
     */
    public static FactualCorrectnessVerdict parseFactualCorrectness(String response, String modelName) {
        Score f1 = new Score(extractNamedScore(response, SCORE_PATTERN));
        Score precision = new Score(extractNamedScore(response, PRECISION_PATTERN));
        Score recall = new Score(extractNamedScore(response, RECALL_PATTERN));
        String rationale = extractRationale(response, response.trim());
        return new FactualCorrectnessVerdict(f1, precision, recall, rationale, modelName);
    }

    private static double extractNamedScore(String response, Pattern pattern) {
        Matcher matcher = pattern.matcher(response);
        if (!matcher.find()) {
            return 0.0;
        }
        double score = Double.parseDouble(matcher.group(1));
        return Math.min(1.0, Math.max(0.0, score));
    }

    private static double extractScore(String response) {
        return extractNamedScore(response, SCORE_PATTERN);
    }

    private static String extractRationale(String response, String fallback) {
        Matcher matcher = RATIONALE_PATTERN.matcher(response);
        if (!matcher.find()) {
            return fallback;
        }
        return matcher.group(1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .trim();
    }

    private static List<Statement> extractStatements(String response) {
        Matcher startMatcher = STATEMENTS_START_PATTERN.matcher(response);
        if (!startMatcher.find()) {
            return List.of();
        }
        int arrayStart = startMatcher.end() - 1;
        int arrayEnd = findMatchingBracket(response, arrayStart);
        if (arrayEnd == -1) {
            return List.of();
        }
        return parseStatementObjects(response.substring(arrayStart + 1, arrayEnd));
    }

    private static int findMatchingBracket(String s, int openPos) {
        int depth = 0;
        for (int i = openPos; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static List<Statement> parseStatementObjects(String arrayContent) {
        List<Statement> statements = new ArrayList<>();
        int depth = 0;
        int start = -1;
        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            if (c == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start != -1) {
                    parseStatement(arrayContent.substring(start, i + 1))
                            .ifPresent(statements::add);
                    start = -1;
                }
            }
        }
        return List.copyOf(statements);
    }

    private static Optional<Statement> parseStatement(String obj) {
        Matcher textMatcher = TEXT_PATTERN.matcher(obj);
        Matcher supportedMatcher = SUPPORTED_PATTERN.matcher(obj);
        if (!textMatcher.find() || !supportedMatcher.find()) {
            return Optional.empty();
        }
        String text = textMatcher.group(1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
        boolean supported = Boolean.parseBoolean(supportedMatcher.group(1));
        return Optional.of(new Statement(text, supported));
    }

    private static List<ChunkVerdict> extractChunkVerdicts(String response, List<Document> context) {
        Matcher startMatcher = CHUNKS_START_PATTERN.matcher(response);
        if (!startMatcher.find()) {
            return List.of();
        }
        int arrayStart = startMatcher.end() - 1;
        int arrayEnd = findMatchingBracket(response, arrayStart);
        if (arrayEnd == -1) {
            return List.of();
        }
        return parseChunkObjects(response.substring(arrayStart + 1, arrayEnd), context);
    }

    private static List<ChunkVerdict> parseChunkObjects(String arrayContent, List<Document> context) {
        List<ChunkVerdict> results = new ArrayList<>();
        int depth = 0;
        int start = -1;
        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            if (c == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start != -1) {
                    parseChunkVerdict(arrayContent.substring(start, i + 1), context)
                            .ifPresent(results::add);
                    start = -1;
                }
            }
        }
        return List.copyOf(results);
    }

    private static Optional<ChunkVerdict> parseChunkVerdict(String obj, List<Document> context) {
        Matcher rankMatcher = RANK_PATTERN.matcher(obj);
        Matcher relevantMatcher = RELEVANT_PATTERN.matcher(obj);
        if (!rankMatcher.find() || !relevantMatcher.find()) {
            return Optional.empty();
        }
        int rank = Integer.parseInt(rankMatcher.group(1));
        boolean relevant = Boolean.parseBoolean(relevantMatcher.group(1));
        if (rank < 1 || rank > context.size()) {
            return Optional.empty();
        }
        return Optional.of(new ChunkVerdict(context.get(rank - 1), relevant, rank));
    }
}
