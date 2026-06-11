package org.ragunit.core.domain;

import java.util.Objects;

/**
 * Represents a single tool invocation in an agentic RAG pipeline.
 *
 * <p>A trajectory is a sequence of {@code ToolCall}s produced by the Generator
 * before arriving at the final {@link Answer}.
 *
 * @param name   the tool identifier (e.g. {@code "web_search"}, {@code "calculator"})
 * @param input  the arguments passed to the tool
 * @param output the result returned by the tool
 */
public record ToolCall(String name, String input, String output) {

    /** Validates that all fields are non-null. */
    public ToolCall {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
    }
}
