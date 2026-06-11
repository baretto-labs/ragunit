package org.ragunit.example.vanilla;

import org.junit.jupiter.api.Test;
import org.ragunit.core.RagTest;
import org.ragunit.core.assertion.RagAssert;
import org.ragunit.core.domain.Answer;
import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.ReferenceAnswer;
import org.ragunit.core.domain.Verdict;
import org.ragunit.core.judge.OllamaJudge;
import org.ragunit.core.judge.RagJudge;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Realistic RAG evaluation example: cloud API support assistant — plain Java 17, no framework.
 *
 * <p>Demonstrates six evaluation scenarios on an 8-document API documentation corpus:
 * happy paths (faithfulness, relevancy, context precision, context recall),
 * a hallucination failure case, and a justified refusal.
 *
 * <p>Prerequisites: Ollama running on localhost:11434 with {@code qwen2.5:14b} pulled.
 * <pre>ollama pull qwen2.5:14b</pre>
 *
 * <p>Run: {@code mvn -pl ragunit-examples/ragunit-example-vanilla -Dgroups=rag-eval test}
 */
class ApiDocRagEvaluationTest {

    private final RagJudge judge = new OllamaJudge("qwen2.5:14b");

    @Test
    @RagTest
    void should_score_high_faithfulness_when_answer_is_grounded_in_auth_docs() {
        var question = new Question("How do I obtain an API access token?");
        var answer = new Answer("""
                To obtain an access token, send a POST request to /auth/token with your \
                client_id and client_secret. Access tokens expire after 3600 seconds; use \
                your refresh token to renew them. Refresh tokens are valid for 30 days.""");

        RagAssert.assertThatAnswer(answer)
                .givenContext(List.of(ApiDocCorpus.AUTH))
                .forQuestion(question)
                .evaluatedBy(judge)
                .isFaithfulToContext(0.85);
    }

    @Test
    @RagTest
    void should_score_high_relevancy_when_answer_addresses_rate_limit_question() {
        var question = new Question("What happens if I exceed the API rate limit?");
        var answer = new Answer("""
                When you exceed your rate limit, the API returns HTTP 429 Too Many Requests \
                with a Retry-After header. Implement exponential backoff with jitter to avoid \
                synchronized retries.""");

        RagAssert.assertThatAnswer(answer)
                .forQuestion(question)
                .evaluatedBy(judge)
                .isRelevantToQuestion(0.85);
    }

    @Test
    @RagTest
    void should_score_high_precision_when_relevant_doc_is_ranked_first() {
        var question = new Question("What are the rate limits per subscription plan?");
        List<Document> contextWithNoise = List.of(
                ApiDocCorpus.RATE_LIMITING,  // rank 1 — relevant
                ApiDocCorpus.MOBILE_SDK,     // rank 2 — noise
                ApiDocCorpus.SLA             // rank 3 — noise
        );

        RagAssert.assertThatContext(contextWithNoise)
                .forQuestion(question)
                .evaluatedBy(judge)
                .hasContextPrecision(0.70);
    }

    @Test
    @RagTest
    void should_score_high_recall_when_context_covers_reference_claims() {
        var question = new Question("What error format does the API use?");
        var reference = new ReferenceAnswer("""
                API errors follow RFC 7807 Problem Details format. Each response includes \
                type, title, status, detail, and instance fields. Common error codes are \
                400, 401, 403, 404, and 429.""");

        RagAssert.assertThatContext(List.of(ApiDocCorpus.ERRORS))
                .forQuestion(question)
                .evaluatedBy(judge)
                .hasContextRecall(reference, 0.80);
    }

    @Test
    @RagTest
    void should_score_low_faithfulness_when_answer_hallucinates_token_expiry() {
        var question = new Question("How long are API tokens valid?");
        var hallucinatedAnswer = new Answer("""
                API tokens are valid based on your plan. Standard tokens expire after 3600 \
                seconds. On the Enterprise plan, tokens never expire and remain permanently \
                valid, eliminating the need for token refresh workflows entirely.""");

        Verdict verdict = judge.evaluateGeneration(
                question, List.of(ApiDocCorpus.AUTH), hallucinatedAnswer);

        assertThat(verdict.score().value())
                .as("Hallucinated 'Enterprise tokens never expire' claim should yield a low score")
                .isLessThan(0.40);
    }

    @Test
    @RagTest
    void should_score_high_rejection_when_context_lacks_mobile_sdk_info() {
        var question = new Question("How do I add the iOS SDK to my Xcode project using CocoaPods?");
        var refusalAnswer = new Answer("""
                I cannot answer this question based on the available documentation. \
                The provided context covers REST API authentication, rate limiting, and \
                pagination, but does not include mobile SDK installation instructions.""");
        List<Document> restApiContext = List.of(
                ApiDocCorpus.AUTH, ApiDocCorpus.RATE_LIMITING, ApiDocCorpus.PAGINATION);

        RagAssert.assertThatAnswer(refusalAnswer)
                .givenContext(restApiContext)
                .forQuestion(question)
                .evaluatedBy(judge)
                .correctlyRefusedToAnswer(0.80);
    }
}
