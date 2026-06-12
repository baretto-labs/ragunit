package org.ragunit.core.judge;

import org.junit.jupiter.api.Test;
import org.ragunit.core.domain.Answer;
import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.FactualCorrectnessVerdict;
import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.ReferenceAnswer;
import org.ragunit.core.domain.Score;
import org.ragunit.core.domain.ToolCall;
import org.ragunit.core.domain.Verdict;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the default {@code RagJudge.verdictFor(JudgeQuery)} dispatch:
 * a query whose criterion is a built-in {@link MetricType} routes to the
 * corresponding typed evaluation method, so every existing RagJudge
 * implementation supports JudgeQuery for free.
 */
class RagJudgeQueryDispatchTest {

    /** Records which typed method handled the call by tagging the verdict rationale. */
    private static class RecordingRagJudge implements RagJudge {

        private Verdict tagged(String method) {
            return Verdict.of(new Score(0.9), method, "recording");
        }

        @Override
        public Verdict evaluateRetrieval(Question q, List<Document> c) {
            return tagged("evaluateRetrieval");
        }

        @Override
        public Verdict evaluateGeneration(Question q, List<Document> c, Answer a) {
            return tagged("evaluateGeneration");
        }

        @Override
        public Verdict evaluateContextRejection(Question q, List<Document> c) {
            return tagged("evaluateContextRejection");
        }

        @Override
        public Verdict evaluateRejection(Question q, List<Document> c, Answer a) {
            return tagged("evaluateRejection");
        }

        @Override
        public Verdict evaluateContextPromptInjection(Question q, List<Document> c) {
            return tagged("evaluateContextPromptInjection");
        }

        @Override
        public Verdict evaluatePromptInjection(Question q, List<Document> c, Answer a) {
            return tagged("evaluatePromptInjection");
        }

        @Override
        public Verdict evaluateContextPIILeak(Question q, List<Document> c) {
            return tagged("evaluateContextPIILeak");
        }

        @Override
        public Verdict evaluatePIILeak(Question q, List<Document> c, Answer a) {
            return tagged("evaluatePIILeak");
        }

        @Override
        public Verdict evaluateAnswerRelevancy(Question q, Answer a) {
            return tagged("evaluateAnswerRelevancy");
        }

        @Override
        public Verdict evaluateContextPrecision(Question q, List<Document> c) {
            return tagged("evaluateContextPrecision");
        }

        @Override
        public Verdict evaluateContextRecall(Question q, List<Document> c, ReferenceAnswer r) {
            return tagged("evaluateContextRecall");
        }

        @Override
        public FactualCorrectnessVerdict evaluateFactualCorrectness(
                Question q, Answer a, ReferenceAnswer r) {
            return new FactualCorrectnessVerdict(new Score(0.9), new Score(0.9), new Score(0.9),
                    "evaluateFactualCorrectness", "recording");
        }

        @Override
        public Verdict evaluateToolTrajectory(Question q, List<ToolCall> t, Answer a) {
            return tagged("evaluateToolTrajectory");
        }
    }

    private static final RagJudge JUDGE = new RecordingRagJudge();

    private static JudgeQuery query(MetricType metric) {
        return JudgeQuery.builder()
                .criterion(metric)
                .input(JudgeQuery.INPUT_QUESTION, "What is the capital of France?")
                .input(JudgeQuery.INPUT_CONTEXT, List.of("Paris is the capital."))
                .input(JudgeQuery.INPUT_ANSWER, "Paris.")
                .input(JudgeQuery.INPUT_REFERENCE, "The capital of France is Paris.")
                .build();
    }

    @Test
    void should_passReconstructedInputs_when_dispatching() {
        var captured = new java.util.concurrent.atomic.AtomicReference<Object[]>();
        RagJudge capturingJudge = new RecordingRagJudge() {
            @Override
            public Verdict evaluateGeneration(Question q, List<Document> c, Answer a) {
                captured.set(new Object[] {q, c, a});
                return super.evaluateGeneration(q, c, a);
            }
        };

        capturingJudge.verdictFor(query(MetricType.GENERATION));

        assertThat(captured.get()[0]).isEqualTo(new Question("What is the capital of France?"));
        assertThat(captured.get()[1]).isEqualTo(List.of(new Document("Paris is the capital.")));
        assertThat(captured.get()[2]).isEqualTo(new Answer("Paris."));
    }

    @Test
    void should_passReconstructedReference_when_dispatchingContextRecall() {
        var captured = new java.util.concurrent.atomic.AtomicReference<ReferenceAnswer>();
        RagJudge capturingJudge = new RecordingRagJudge() {
            @Override
            public Verdict evaluateContextRecall(Question q, List<Document> c, ReferenceAnswer r) {
                captured.set(r);
                return super.evaluateContextRecall(q, c, r);
            }
        };

        capturingJudge.verdictFor(query(MetricType.CONTEXT_RECALL));

        assertThat(captured.get()).isEqualTo(new ReferenceAnswer("The capital of France is Paris."));
    }

    @Test
    void should_dispatchToTypedMethod_when_criterionIsBuiltInMetric() {
        assertThat(JUDGE.verdictFor(query(MetricType.RETRIEVAL)).rationale())
                .isEqualTo("evaluateRetrieval");
        assertThat(JUDGE.verdictFor(query(MetricType.GENERATION)).rationale())
                .isEqualTo("evaluateGeneration");
        assertThat(JUDGE.verdictFor(query(MetricType.ANSWER_RELEVANCY)).rationale())
                .isEqualTo("evaluateAnswerRelevancy");
        assertThat(JUDGE.verdictFor(query(MetricType.CONTEXT_PRECISION)).rationale())
                .isEqualTo("evaluateContextPrecision");
        assertThat(JUDGE.verdictFor(query(MetricType.CONTEXT_RECALL)).rationale())
                .isEqualTo("evaluateContextRecall");
    }

    @Test
    void should_dispatchSafetyMetrics_when_criterionIsBuiltInMetric() {
        assertThat(JUDGE.verdictFor(query(MetricType.CONTEXT_REJECTION)).rationale())
                .isEqualTo("evaluateContextRejection");
        assertThat(JUDGE.verdictFor(query(MetricType.REJECTION)).rationale())
                .isEqualTo("evaluateRejection");
        assertThat(JUDGE.verdictFor(query(MetricType.CONTEXT_PROMPT_INJECTION)).rationale())
                .isEqualTo("evaluateContextPromptInjection");
        assertThat(JUDGE.verdictFor(query(MetricType.PROMPT_INJECTION)).rationale())
                .isEqualTo("evaluatePromptInjection");
        assertThat(JUDGE.verdictFor(query(MetricType.CONTEXT_PII_LEAK)).rationale())
                .isEqualTo("evaluateContextPIILeak");
        assertThat(JUDGE.verdictFor(query(MetricType.PII_LEAK)).rationale())
                .isEqualTo("evaluatePIILeak");
    }

    @Test
    void should_returnJudgeResult_when_evaluatingQuery() {
        JudgeResult result = JUDGE.evaluate(query(MetricType.GENERATION));

        assertThat(result.score()).isEqualTo(0.9);
        assertThat(result.justification()).isEqualTo("evaluateGeneration");
    }

    @Test
    void should_throwJudgeException_when_criterionIsNotBuiltIn() {
        JudgeQuery custom = JudgeQuery.builder()
                .criterion(Criterion.of("conciseness", "Is it concise?"))
                .input("summary", "text")
                .build();

        assertThatThrownBy(() -> JUDGE.verdictFor(custom))
                .isInstanceOf(JudgeException.class)
                .hasMessageContaining("conciseness");
    }

    @Test
    void should_throwJudgeException_when_metricNeedsInputsTheQueryLacks() {
        JudgeQuery withoutQuestion = JudgeQuery.builder()
                .criterion(MetricType.GENERATION)
                .input(JudgeQuery.INPUT_ANSWER, "Paris.")
                .build();

        assertThatThrownBy(() -> JUDGE.verdictFor(withoutQuestion))
                .isInstanceOf(JudgeException.class)
                .hasMessageContaining(JudgeQuery.INPUT_QUESTION);
    }

    @Test
    void should_throwJudgeException_when_metricIsNotQueryDispatchable() {
        assertThatThrownBy(() -> JUDGE.verdictFor(query(MetricType.TOOL_TRAJECTORY)))
                .isInstanceOf(JudgeException.class);
    }

    @Test
    void should_mapFactualCorrectnessF1ToScore_when_dispatchingFactualCorrectness() {
        Verdict verdict = JUDGE.verdictFor(query(MetricType.FACTUAL_CORRECTNESS));

        assertThat(verdict.score().value()).isEqualTo(0.9);
        assertThat(verdict.rationale()).isEqualTo("evaluateFactualCorrectness");
    }
}
