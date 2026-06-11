package org.ragunit.core.testset;

import org.ragunit.core.domain.Testset;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Loads a {@link Testset} from a previously exported file.
 *
 * <p>Implementations: {@link CsvTestsetImporter}, {@link JsonTestsetImporter}.
 *
 * <p>Round-trip guarantee: {@code load(export(testset))} produces an equal testset.
 */
public interface TestsetImporter {

    /**
     * Loads a testset from the given source path.
     *
     * @param source the input file path
     * @return the loaded testset
     * @throws IOException if the file cannot be read or is malformed
     */
    Testset load(Path source) throws IOException;
}
