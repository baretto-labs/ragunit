/**
 * Judge abstraction and its implementations.
 *
 * <p>The central interface is {@code RagJudge}: given a {@code Question}, a {@code Context},
 * and optionally an {@code Answer}, it produces a {@code Verdict}.
 *
 * <p>v0.1 implementation: {@code OllamaJudge} — uses {@code java.net.http.HttpClient}
 * to call a local Ollama instance (default port 11434). Zero external dependencies.
 */
package org.ragunit.core.judge;
