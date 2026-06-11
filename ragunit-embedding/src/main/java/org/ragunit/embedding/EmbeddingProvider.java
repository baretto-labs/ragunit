package org.ragunit.embedding;

/**
 * Strategy for computing a dense vector representation (embedding) of a text string.
 *
 * <p>Implementations call an embedding model and return a normalized float array.
 * The dimension of the returned vector is model-dependent and must be consistent
 * across all calls within a single evaluation run.
 *
 * @see OllamaEmbeddingProvider
 */
@FunctionalInterface
public interface EmbeddingProvider {

    /**
     * Computes the embedding vector for the given text.
     *
     * @param text the input text (must not be null or blank)
     * @return a float array representing the embedding; length is model-dependent
     * @throws EmbeddingException if the provider fails to compute the embedding
     */
    float[] embed(String text);
}
