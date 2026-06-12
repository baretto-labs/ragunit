package org.ragunit.core.judge;

import org.ragunit.core.domain.Answer;
import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.FactualCorrectnessVerdict;
import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.ReferenceAnswer;
import org.ragunit.core.domain.Verdict;

import java.util.List;

/**
 * Routes a {@link JudgeQuery} whose criterion is a built-in {@link MetricType}
 * to the corresponding typed method of a {@link RagJudge}, reconstructing the
 * domain inputs from the query's canonical named inputs.
 *
 * <p>This is what makes every existing RagJudge implementation support
 * {@code evaluate(JudgeQuery)} without code changes.
 */
final class JudgeQueryDispatch {

    private JudgeQueryDispatch() {
    }

    static Verdict verdictFor(RagJudge judge, JudgeQuery query) {
        if (!(query.criterion() instanceof MetricType metric)) {
            throw new JudgeException(("Criterion '%s' is not a built-in metric — "
                    + "override verdictFor(JudgeQuery) to support arbitrary criteria")
                    .formatted(query.criterion().name()));
        }
        Question question = new Question(query.requiredInput(JudgeQuery.INPUT_QUESTION));
        List<Document> context = query.inputValues(JudgeQuery.INPUT_CONTEXT).stream()
                .map(Document::new).toList();
        return switch (metric) {
            case RETRIEVAL -> judge.evaluateRetrieval(question, context);
            case GENERATION -> judge.evaluateGeneration(question, context, answerFrom(query));
            case ANSWER_RELEVANCY -> judge.evaluateAnswerRelevancy(question, answerFrom(query));
            case CONTEXT_PRECISION -> judge.evaluateContextPrecision(question, context);
            case CONTEXT_RECALL -> judge.evaluateContextRecall(question, context, referenceFrom(query));
            case FACTUAL_CORRECTNESS -> factualVerdict(judge, question, query);
            case CONTEXT_REJECTION -> judge.evaluateContextRejection(question, context);
            case REJECTION -> judge.evaluateRejection(question, context, answerFrom(query));
            case CONTEXT_PROMPT_INJECTION -> judge.evaluateContextPromptInjection(question, context);
            case PROMPT_INJECTION -> judge.evaluatePromptInjection(question, context, answerFrom(query));
            case CONTEXT_PII_LEAK -> judge.evaluateContextPIILeak(question, context);
            case PII_LEAK -> judge.evaluatePIILeak(question, context, answerFrom(query));
            case TOOL_TRAJECTORY -> throw new JudgeException(
                    "TOOL_TRAJECTORY is not query-dispatchable — call evaluateToolTrajectory directly");
        };
    }

    private static Verdict factualVerdict(RagJudge judge, Question question, JudgeQuery query) {
        FactualCorrectnessVerdict factual = judge.evaluateFactualCorrectness(
                question, answerFrom(query), referenceFrom(query));
        return Verdict.of(factual.f1(), factual.rationale(), factual.model());
    }

    private static Answer answerFrom(JudgeQuery query) {
        return new Answer(query.requiredInput(JudgeQuery.INPUT_ANSWER));
    }

    private static ReferenceAnswer referenceFrom(JudgeQuery query) {
        return new ReferenceAnswer(query.requiredInput(JudgeQuery.INPUT_REFERENCE));
    }
}
