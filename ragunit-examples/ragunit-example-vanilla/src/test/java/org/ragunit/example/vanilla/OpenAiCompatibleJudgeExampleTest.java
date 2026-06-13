package org.ragunit.example.vanilla;

import org.junit.jupiter.api.Test;
import org.ragunit.core.RagTest;
import org.ragunit.core.assertion.RagAssert;
import org.ragunit.core.domain.Answer;
import org.ragunit.core.domain.Question;
import org.ragunit.core.judge.RagJudge;
import org.ragunit.cloud.OpenAiCompatibleJudge;

import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Example: using a hosted, OpenAI-compatible model as the judge instead of local Ollama.
 *
 * <p>The very same fluent API and metrics apply — only the judge changes. One
 * {@link OpenAiCompatibleJudge} works with OpenAI, Azure, Groq, Together, OpenRouter,
 * and Anthropic Claude / Google Gemini via their OpenAI-compatible endpoints; just
 * change {@code baseUrl} and {@code model}.
 *
 * <p>This test is skipped unless {@code OPENAI_API_KEY} is set (and is tagged
 * {@code @RagTest}, so it never runs in fast CI). Point {@code RAGUNIT_OPENAI_BASE_URL}
 * and {@code RAGUNIT_OPENAI_MODEL} at any compatible provider to retarget it.
 */
class OpenAiCompatibleJudgeExampleTest {

    @Test
    @RagTest
    void should_judge_faithfulness_with_a_hosted_openai_compatible_model() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        assumeTrue(apiKey != null && !apiKey.isBlank(),
                "Set OPENAI_API_KEY to run this example");

        var answer = new Answer("""
                When you exceed your rate limit, the API returns HTTP 429 Too Many Requests \
                with a Retry-After header indicating how long to wait before retrying.""");

        RagAssert.assertThatAnswer(answer)
                .contains("429")                       // deterministic hard check, no API call
                .givenContext(List.of(ApiDocCorpus.RATE_LIMITING))
                .forQuestion(new Question("What happens if I exceed the API rate limit?"))
                .evaluatedBy(hostedJudge(apiKey))
                .withRuns(3)                           // average 3 runs; frontier models are stable
                .isFaithfulToContext(0.80);
    }

    /** One judge for any OpenAI-compatible provider — retarget via env vars. */
    private static RagJudge hostedJudge(String apiKey) {
        return OpenAiCompatibleJudge.builder()
                .baseUrl(envOrDefault("RAGUNIT_OPENAI_BASE_URL", "https://api.openai.com/v1"))
                .apiKey(apiKey)
                .model(envOrDefault("RAGUNIT_OPENAI_MODEL", "gpt-4o-mini"))
                .build();
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
