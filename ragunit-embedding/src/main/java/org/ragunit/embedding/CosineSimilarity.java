package org.ragunit.embedding;

import java.util.Objects;

/**
 * Computes the cosine similarity between two embedding vectors.
 *
 * <p>Cosine similarity measures the cosine of the angle between two vectors,
 * returning a value in [-1.0, 1.0]. A value of 1.0 means the vectors point in
 * the same direction (identical meaning), 0.0 means orthogonal (unrelated),
 * and -1.0 means opposite directions.
 *
 * <p>For normalized embeddings (unit vectors), this is equivalent to the dot product.
 */
final class CosineSimilarity {

    private static final double ZERO_THRESHOLD = 1e-10;

    private CosineSimilarity() {
    }

    /**
     * Computes the cosine similarity between two vectors.
     *
     * @param a the first vector
     * @param b the second vector
     * @return cosine similarity in [-1.0, 1.0]
     * @throws IllegalArgumentException if vectors have different dimensions or either is a zero vector
     * @throws NullPointerException     if either vector is null
     */
    static double compute(float[] a, float[] b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        if (a.length != b.length) {
            throw new IllegalArgumentException(
                    "Vectors must have the same dimension: " + a.length + " vs " + b.length);
        }
        double normA = norm(a, "First");
        double normB = norm(b, "Second");
        double dot = dot(a, b);
        return dot / (normA * normB);
    }

    private static double dot(float[] a, float[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += (double) a[i] * b[i];
        }
        return sum;
    }

    private static double norm(float[] v, String label) {
        double sum = 0.0;
        for (float x : v) {
            sum += (double) x * x;
        }
        double n = Math.sqrt(sum);
        if (n < ZERO_THRESHOLD) {
            throw new IllegalArgumentException(label + " vector is a zero vector");
        }
        return n;
    }
}
