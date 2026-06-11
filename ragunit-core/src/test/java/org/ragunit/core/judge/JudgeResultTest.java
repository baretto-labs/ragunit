package org.ragunit.core.judge;

import org.junit.jupiter.api.Test;
import org.ragunit.core.domain.Score;
import org.ragunit.core.domain.Verdict;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JudgeResult}: the structured, inspection-friendly view of a
 * judge evaluation — score, justification, exact prompt, and raw response.
 */
class JudgeResultTest {

    @Test
    void should_mapAllFields_when_builtFromVerdictWithExchange() {
        Verdict verdict = Verdict.of(new Score(0.8), "3 of 4 claims supported.", "qwen2.5:14b")
                .withExchange("THE PROMPT", "{\"score\": 0.8}");

        JudgeResult result = JudgeResult.fromVerdict(verdict);

        assertThat(result.score()).isEqualTo(0.8);
        assertThat(result.justification()).isEqualTo("3 of 4 claims supported.");
        assertThat(result.promptUsed()).isEqualTo("THE PROMPT");
        assertThat(result.rawResponse()).isEqualTo("{\"score\": 0.8}");
        assertThat(result.model()).isEqualTo("qwen2.5:14b");
    }

    @Test
    void should_useEmptyStrings_when_verdictHasNoExchange() {
        Verdict verdict = Verdict.of(new Score(0.5), "rationale", "model");

        JudgeResult result = JudgeResult.fromVerdict(verdict);

        assertThat(result.promptUsed()).isEmpty();
        assertThat(result.rawResponse()).isEmpty();
    }
}
