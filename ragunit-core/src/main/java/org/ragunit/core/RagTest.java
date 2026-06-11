package org.ragunit.core;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test method as an expensive RAG evaluation test backed by a live LLM.
 *
 * <p>Combines {@link Test} and {@code @Tag("rag-eval")} so these tests can be
 * included or excluded from CI pipelines without changing test code:
 * <pre>
 *   mvn test -Dgroups="!rag-eval"   # exclude all @RagTest (fast CI)
 *   mvn test -Dgroups="rag-eval"    # run only @RagTest (LLM integration suite)
 * </pre>
 *
 * <p>Usage:
 * <pre>{@code
 * @RagTest
 * void should_returnFaithfulVerdict_when_answerMatchesContext() {
 *     // calls Ollama — requires a running local model
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Test
@Tag("rag-eval")
public @interface RagTest {
}
