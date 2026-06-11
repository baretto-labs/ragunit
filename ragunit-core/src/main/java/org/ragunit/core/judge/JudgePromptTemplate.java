package org.ragunit.core.judge;

import org.ragunit.core.domain.PromptContext;

/**
 * A functional interface for providing domain-specific prompt templates to a {@link RagJudge}.
 *
 * <p>Implementations receive a {@link PromptContext} containing the question, retrieved
 * context, and optional answer, and return the full prompt string to send to the LLM.
 *
 * <p>Usage (legal domain example):
 * <pre>{@code
 * JudgePromptTemplate legalTemplate = ctx ->
 *     "You are evaluating a legal RAG system. Cite case law strictly.\n" +
 *     "Question: " + ctx.question().text() + "\n" + ...;
 *
 * OllamaJudge judge = new OllamaJudge("qwen2.5:14b",
 *     Map.of(MetricType.GENERATION, legalTemplate));
 * }</pre>
 *
 * <p>When no template is configured for a {@link MetricType}, the judge falls back to
 * its built-in default prompt.
 */
@FunctionalInterface
public interface JudgePromptTemplate {

    /**
     * Renders a prompt string from the given evaluation context.
     *
     * @param context the inputs available to the judge for this evaluation
     * @return the full prompt string to send to the LLM
     */
    String render(PromptContext context);
}
