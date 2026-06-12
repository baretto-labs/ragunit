package org.ragunit.example.vanilla;

import org.junit.jupiter.api.Test;
import org.ragunit.core.RagTest;
import org.ragunit.core.assertion.RagAssert;
import org.ragunit.core.domain.Answer;
import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.Question;
import org.ragunit.core.judge.Criterion;
import org.ragunit.core.judge.JudgePromptTemplate;
import org.ragunit.core.judge.JudgeQuery;
import org.ragunit.core.judge.JudgeResult;
import org.ragunit.core.judge.OllamaJudge;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The v0.2 measurement foundation, end to end in one test class:
 * <ol>
 *   <li><b>Custom prompt</b> — the faithfulness prompt is replaced via the builder,
 *       so the question asked to the judge is fully under user control;</li>
 *   <li><b>N-runs variance control</b> — the assertion averages 3 judge runs and
 *       fails if the judge is too unstable, making the score defensible;</li>
 *   <li><b>Generic JudgeQuery</b> — a non-RAG criterion is evaluated directly,
 *       and the full {@link JudgeResult} (prompt, raw response, justification)
 *       is available for inspection.</li>
 * </ol>
 *
 * <p>Prerequisites: Ollama running on localhost:11434 with {@code qwen2.5:14b} pulled.
 * <pre>ollama pull qwen2.5:14b</pre>
 *
 * <p>Run: {@code mvn -pl ragunit-examples/ragunit-example-vanilla -Dgroups=rag-eval test}
 */
class MeasurementFoundationExampleTest {

    /** Custom faithfulness prompt: stricter than the default, tuned for API support. */
    private static final JudgePromptTemplate STRICT_FAITHFULNESS = ctx -> """
            You are a strict technical-documentation judge. A claim counts as \
            supported ONLY if the context states it explicitly — no inference.

            Question: %s

            Context:
            %s

            Answer: %s

            Rate how faithful the answer is to the context.
            Reply ONLY with a valid JSON object — no markdown, no code block:
            {"score": <float 0.0-1.0>, "rationale": "<one sentence>", "statements": []}
            """.formatted(
            ctx.question().text(),
            ctx.retrievedContext().stream()
                    .map(Document::content)
                    .collect(Collectors.joining("\n")),
            ctx.answer().map(Answer::text).orElse(""));

    private final OllamaJudge judge = OllamaJudge.builder()
            .model("qwen2.5:14b")
            .faithfulnessPrompt(STRICT_FAITHFULNESS)   // T1.1 — visible, overridable prompt
            .build();                                  // temperature 0 by default (T1.2)

    @Test
    @RagTest
    void should_measure_faithfulness_reproducibly_with_custom_prompt_and_three_runs() {
        var question = new Question("How do I obtain an API access token?");
        var answer = new Answer("""
                Send a POST request to /auth/token with your client_id and client_secret. \
                The access token expires after 3600 seconds.""");

        RagAssert.assertThatAnswer(answer)
                .contains("/auth/token")               // T2.2 — hard check first, zero cost
                .givenContext(List.of(ApiDocCorpus.AUTH))
                .forQuestion(question)
                .evaluatedBy(judge)
                .withRuns(3)                           // T1.2 — mean of 3 runs, stddev bound
                .isFaithfulToContext(0.80);
    }

    @Test
    @RagTest
    void should_judge_a_non_rag_output_and_expose_the_full_judge_result() {
        JudgeQuery query = JudgeQuery.builder()       // T2.1 — generic, non-RAG-shaped
                .criterion(Criterion.of("changelog-quality",
                        "Is this changelog entry clear, specific, and actionable for users?"))
                .input("Changelog entry",
                        "Fixed a bug where token refresh failed after 30 days of inactivity; "
                        + "clients should upgrade to SDK 2.4.1 or renew tokens manually.")
                .build();

        JudgeResult result = judge.evaluate(query);   // T1.3 — structured result

        assertThat(result.promptUsed()).contains("changelog entry");
        assertThat(result.justification()).isNotBlank();
        assertThat(result.score()).isGreaterThanOrEqualTo(0.7);
    }
}
