package org.ragunit.core.testset;

import org.ragunit.core.domain.Testset;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Persists a {@link Testset} to a file in a specific format.
 *
 * <p>Implementations: {@link CsvTestsetExporter}, {@link JsonTestsetExporter}.
 *
 * <p>Exporting testsets enables CI reuse: generate once, persist, re-evaluate on every run
 * without paying the cost of LLM generation on each CI build.
 */
public interface TestsetExporter {

    /**
     * Exports the testset to the given destination path.
     *
     * @param testset     the testset to export
     * @param destination the output file path (parent directories are created if absent)
     * @throws IOException if the file cannot be written
     */
    void export(Testset testset, Path destination) throws IOException;
}
