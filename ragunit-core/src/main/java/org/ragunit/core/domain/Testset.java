package org.ragunit.core.domain;

import java.util.List;
import java.util.Objects;

/**
 * An ordered collection of {@link TestCase} instances produced by a {@code TestsetGenerator}.
 *
 * <p>A {@code Testset} is the primary artifact of the synthetic test data generation pipeline.
 * It can be exported to CSV or JSON for versioning and reuse across CI runs (TASK-020).
 *
 * @param cases the ordered list of test cases; must not be null
 */
public record Testset(List<TestCase> cases) {

    /** Validates {@code cases} is non-null and makes it unmodifiable. */
    public Testset {
        Objects.requireNonNull(cases, "cases");
        cases = List.copyOf(cases);
    }

    /**
     * Returns the number of test cases in this testset.
     *
     * @return the number of test cases
     */
    public int size() {
        return cases.size();
    }

    /**
     * Returns {@code true} if this testset contains no test cases.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return cases.isEmpty();
    }
}
