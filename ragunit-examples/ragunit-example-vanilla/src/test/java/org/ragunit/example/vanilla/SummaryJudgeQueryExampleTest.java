package org.ragunit.example.vanilla;

import org.junit.jupiter.api.Test;
import org.ragunit.core.RagTest;
import org.ragunit.core.judge.Criterion;
import org.ragunit.core.judge.Judge;
import org.ragunit.core.judge.JudgeQuery;
import org.ragunit.core.judge.JudgeResult;
import org.ragunit.core.judge.OllamaJudge;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Non-RAG evaluation example: judging a summary against its source and a reference,
 * using the generic {@link JudgeQuery} entry point — no Question, no Context, no Answer.
 *
 * <p>This is the v0.2 generic API: any (input / output / reference) triplet can be
 * judged with an ad-hoc {@link Criterion}. The full {@link JudgeResult} exposes the
 * exact prompt sent to the judge and the raw LLM response for auditability.
 *
 * <p>Prerequisites: Ollama running on localhost:11434 with {@code qwen2.5:14b} pulled.
 * <pre>ollama pull qwen2.5:14b</pre>
 *
 * <p>Run: {@code mvn -pl ragunit-examples/ragunit-example-vanilla -Dgroups=rag-eval test}
 */
class SummaryJudgeQueryExampleTest {

    private static final String SOURCE_ARTICLE = """
            The European Union's AI Act entered into force in August 2024. It classifies \
            AI systems into four risk tiers: unacceptable, high, limited, and minimal. \
            Unacceptable-risk systems, such as social scoring by public authorities, are \
            banned outright. High-risk systems — including AI used in hiring, credit \
            scoring, and critical infrastructure — must meet strict requirements on data \
            governance, transparency, human oversight, and robustness before entering the \
            market. Providers of general-purpose AI models face additional documentation \
            and copyright-compliance obligations. Most provisions apply from August 2026, \
            with bans on unacceptable-risk systems applying from February 2025.""";

    private static final String CANDIDATE_SUMMARY = """
            The EU AI Act (in force since August 2024) sorts AI systems into four risk \
            tiers. It bans unacceptable-risk uses like social scoring, imposes strict \
            requirements on high-risk systems such as hiring or credit scoring, and adds \
            documentation duties for general-purpose models. Most rules apply from \
            August 2026.""";

    private static final String REFERENCE_SUMMARY = """
            The EU AI Act, effective August 2024, introduces a four-tier risk \
            classification, bans unacceptable-risk AI, regulates high-risk systems \
            strictly, obliges general-purpose model providers to document compliance, \
            and phases in most obligations by August 2026.""";

    private final Judge judge = OllamaJudge.builder().model("qwen2.5:14b").build();

    @Test
    @RagTest
    void should_score_high_when_summary_covers_the_key_points_of_the_source() {
        JudgeQuery query = JudgeQuery.builder()
                .criterion(Criterion.of("summary-completeness",
                        "Does the summary cover all key points of the source without adding facts?"))
                .input("Source", SOURCE_ARTICLE)
                .input("Summary", CANDIDATE_SUMMARY)
                .build();

        JudgeResult result = judge.evaluate(query);

        assertThat(result.score()).isGreaterThanOrEqualTo(0.7);
    }

    @Test
    @RagTest
    void should_expose_prompt_and_justification_when_judging_against_a_reference() {
        JudgeQuery query = JudgeQuery.builder()
                .criterion(Criterion.of("reference-agreement",
                        "Does the candidate summary convey the same information as the reference summary?"))
                .input("Candidate summary", CANDIDATE_SUMMARY)
                .input("Reference summary", REFERENCE_SUMMARY)
                .build();

        JudgeResult result = judge.evaluate(query);

        // The measurement is auditable: prompt, raw response, and justification are all visible.
        assertThat(result.promptUsed()).contains("Candidate summary");
        assertThat(result.rawResponse()).isNotBlank();
        assertThat(result.justification()).isNotBlank();
        assertThat(result.score()).isGreaterThanOrEqualTo(0.7);
    }
}
