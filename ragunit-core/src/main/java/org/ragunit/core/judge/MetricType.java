package org.ragunit.core.judge;

/**
 * Identifies each evaluation metric supported by {@link RagJudge}.
 *
 * <p>Used as the key in the custom-template map passed to judge constructors
 * (e.g. {@code OllamaJudge(model, Map.of(MetricType.GENERATION, myTemplate))}).
 */
public enum MetricType {
    /** Context relevance: is the retrieved context relevant to the question? */
    RETRIEVAL,
    /** Faithfulness: is the answer grounded in the retrieved context? */
    GENERATION,
    /** Answer relevancy: is the answer on-topic for the question? */
    ANSWER_RELEVANCY,
    /** Context precision: are the most relevant chunks ranked first? */
    CONTEXT_PRECISION,
    /** Context recall: does the context cover all claims in the reference answer? */
    CONTEXT_RECALL,
    /** Factual correctness: are the answer's claims accurate relative to the reference answer? */
    FACTUAL_CORRECTNESS,
    /** Rejection: was the generator's refusal justified by the context? */
    REJECTION,
    /** Prompt injection: does the context or answer contain adversarial injection attempts? */
    PROMPT_INJECTION,
    /** PII leak: does the answer expose personally identifiable information? */
    PII_LEAK,
    /** Tool trajectory: were the agentic tool calls necessary, sufficient, and correctly ordered? */
    TOOL_TRAJECTORY
}
