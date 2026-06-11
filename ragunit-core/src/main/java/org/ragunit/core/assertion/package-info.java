/**
 * Fluent assertion API — the public surface of RAGUnit.
 *
 * <p>Entry point: {@code RagAssert}. Two assertion flows:
 * <ul>
 *   <li>{@code RagAssert.assertThatContext(...)} — evaluates Retriever quality</li>
 *   <li>{@code RagAssert.assertThatAnswer(...)} — evaluates Generator quality</li>
 * </ul>
 *
 * <p>Only {@code RagAssert} is public. Builder classes are package-private.
 * Design inspired by AssertJ: each method returns {@code this} for chaining.
 */
package org.ragunit.core.assertion;
