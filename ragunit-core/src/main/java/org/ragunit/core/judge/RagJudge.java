package org.ragunit.core.judge;

import org.ragunit.core.domain.Answer;
import org.ragunit.core.domain.Document;
import org.ragunit.core.domain.FactualCorrectnessVerdict;
import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.ReferenceAnswer;
import org.ragunit.core.domain.ToolCall;
import org.ragunit.core.domain.Verdict;

import java.util.List;

/**
 * Evaluates the quality of a RAG pipeline component using an LLM-as-judge strategy.
 *
 * <p>Supported evaluation flows:
 * <ul>
 *   <li><b>Retrieval</b>: are the retrieved documents relevant to the question?</li>
 *   <li><b>Generation</b>: is the answer faithful to the context?</li>
 *   <li><b>Context rejection</b>: is the context too insufficient to warrant a refusal?</li>
 *   <li><b>Rejection</b>: did the generator rightfully refuse given context and answer?</li>
 * </ul>
 *
 * <p>Implementations must be synchronous. See TASK-004 for {@code OllamaJudge}.
 */
public interface RagJudge {

    /**
     * Evaluates how relevant the retrieved {@code context} is for the given {@code question}.
     *
     * @param question the user's original query
     * @param context  the list of documents retrieved by the Retriever
     * @return a {@link Verdict} with a relevance score and rationale
     */
    Verdict evaluateRetrieval(Question question, List<Document> context);

    /**
     * Evaluates how faithful the {@code answer} is to the {@code context},
     * and how relevant it is to the {@code question}.
     *
     * @param question the user's original query
     * @param context  the list of documents used as context by the Generator
     * @param answer   the text produced by the Generator
     * @return a {@link Verdict} with a faithfulness score and rationale
     */
    Verdict evaluateGeneration(Question question, List<Document> context, Answer answer);

    /**
     * Evaluates whether the context is too insufficient to answer the question,
     * i.e. whether a refusal would be justified from a context-quality perspective.
     *
     * <p>Score semantics: {@code 1.0} = context is too poor, refusal is warranted;
     * {@code 0.0} = context is rich, the generator should have answered.
     *
     * @param question the user's original query
     * @param context  the list of documents retrieved by the Retriever
     * @return a {@link Verdict} with a rejection-justification score
     */
    Verdict evaluateContextRejection(Question question, List<Document> context);

    /**
     * Evaluates whether the generator was right to refuse answering, given both
     * the context quality and the actual refusal text in the answer.
     *
     * <p>Score semantics: {@code 1.0} = refusal perfectly justified;
     * {@code 0.0} = refusal unjustified (context was sufficient).
     *
     * @param question the user's original query
     * @param context  the list of documents used as context by the Generator
     * @param answer   the refusal text produced by the Generator
     * @return a {@link Verdict} with a rejection-justification score
     */
    Verdict evaluateRejection(Question question, List<Document> context, Answer answer);

    /**
     * Evaluates whether the retrieved context contains prompt injection attempts
     * (e.g. "Ignore previous instructions and…").
     *
     * <p>Score semantics: {@code 1.0} = no injection detected (safe);
     * {@code 0.0} = injection detected (dangerous).
     *
     * @param question the user's original query
     * @param context  the list of documents retrieved by the Retriever
     * @return a {@link Verdict} with a safety score
     */
    Verdict evaluateContextPromptInjection(Question question, List<Document> context);

    /**
     * Evaluates whether the generated answer contains or echoes a prompt injection.
     *
     * <p>Score semantics: {@code 1.0} = no injection in the answer (safe);
     * {@code 0.0} = injection detected in the answer (compromised).
     *
     * @param question the user's original query
     * @param context  the list of documents used as context by the Generator
     * @param answer   the text produced by the Generator
     * @return a {@link Verdict} with a safety score
     */
    Verdict evaluatePromptInjection(Question question, List<Document> context, Answer answer);

    /**
     * Evaluates whether the retrieved context exposes personally identifiable information (PII)
     * that should not be surfaced.
     *
     * <p>Score semantics: {@code 1.0} = no PII detected (compliant);
     * {@code 0.0} = PII exposed (violation).
     *
     * @param question the user's original query
     * @param context  the list of documents retrieved by the Retriever
     * @return a {@link Verdict} with a compliance score
     */
    Verdict evaluateContextPIILeak(Question question, List<Document> context);

    /**
     * Evaluates whether the generated answer leaks personally identifiable information (PII).
     *
     * <p>Score semantics: {@code 1.0} = no PII in the answer (compliant);
     * {@code 0.0} = PII leaked in the answer (GDPR violation).
     *
     * @param question the user's original query
     * @param context  the list of documents used as context by the Generator
     * @param answer   the text produced by the Generator
     * @return a {@link Verdict} with a compliance score
     */
    Verdict evaluatePIILeak(Question question, List<Document> context, Answer answer);

    /**
     * Evaluates whether the answer is relevant to the original question using a
     * hypothetical-question approach (LLM-only, no embeddings required).
     *
     * <p>The judge generates N hypothetical questions that the Answer could answer,
     * then assesses whether each aligns semantically with the original Question.
     * {@code statements} holds the hypothetical questions; {@code supported = true}
     * means the hypothetical is aligned with the original.
     * Score = aligned / total.
     *
     * <p>Score semantics: {@code 1.0} = answer perfectly on-topic;
     * {@code 0.0} = answer entirely off-topic.
     *
     * @param question the user's original query
     * @param answer   the text produced by the Generator
     * @return a {@link Verdict} with a relevancy score and hypothetical questions as statements
     */
    Verdict evaluateAnswerRelevancy(Question question, Answer answer);

    /**
     * Evaluates how precisely the retrieved context serves the question,
     * assessing each chunk individually and weighting by rank position.
     *
     * <p>Returns a {@link Verdict} with a {@code chunkVerdicts} list where each entry
     * marks one retrieved document as relevant or not. The declared score follows the
     * RAGAS Average Precision formula: relevant chunks ranked first score highest.
     *
     * @param question the user's original query
     * @param context  the list of documents retrieved by the Retriever (order matters)
     * @return a {@link Verdict} with a precision score and per-chunk relevance breakdown
     */
    Verdict evaluateContextPrecision(Question question, List<Document> context);

    /**
     * Evaluates how well the retrieved context covers the claims in a reference answer
     * (ground truth). The judge decomposes the reference into claims and checks whether
     * each is supported by at least one document in the context.
     *
     * <p>Score = covered claims / total reference claims.
     * A low score indicates the Retriever missed documents essential to answering correctly.
     *
     * @param question  the user's original query
     * @param context   the list of documents retrieved by the Retriever
     * @param reference the ground-truth answer to compare against
     * @return a {@link Verdict} with a recall score and per-claim breakdown in statements
     */
    Verdict evaluateContextRecall(Question question, List<Document> context, ReferenceAnswer reference);

    /**
     * Evaluates the factual correctness of the answer against a ground-truth reference,
     * computing precision, recall, and F1 at the claim level.
     *
     * <p>The judge decomposes both the Answer and the ReferenceAnswer into atomic claims,
     * then cross-checks them:
     * <ul>
     *   <li>Precision = Answer claims supported by the Reference / total Answer claims</li>
     *   <li>Recall    = Reference claims covered by the Answer / total Reference claims</li>
     *   <li>F1        = harmonic mean of precision and recall</li>
     * </ul>
     *
     * <p>A pipeline can be faithful (no hallucination w.r.t. the context) but score
     * low on FactualCorrectness when the context itself is incomplete or erroneous.
     *
     * @param question  the user's original query
     * @param answer    the text produced by the Generator
     * @param reference the ground-truth answer to compare against
     * @return a {@link FactualCorrectnessVerdict} with F1, precision, and recall scores
     */
    FactualCorrectnessVerdict evaluateFactualCorrectness(
            Question question, Answer answer, ReferenceAnswer reference);

    /**
     * Evaluates whether the sequence of tool calls is necessary, sufficient,
     * and in the correct order to answer the question.
     *
     * <p>Score semantics: {@code 1.0} = trajectory optimal (all required tools, correct order);
     * {@code 0.0} = trajectory invalid (missing, redundant, or out-of-order tool calls).
     *
     * @param question   the user's original query
     * @param trajectory the ordered list of tool calls produced by the Generator
     * @param answer     the final text produced by the Generator
     * @return a {@link Verdict} with a trajectory quality score
     */
    Verdict evaluateToolTrajectory(Question question, List<ToolCall> trajectory, Answer answer);
}
