package org.ragunit.embedding;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class CosineSimilarityTest {

    @Test
    void should_return_1_when_vectors_are_identical() {
        float[] v = {1.0f, 0.0f, 0.0f};
        assertThat(CosineSimilarity.compute(v, v)).isCloseTo(1.0, within(1e-6));
    }

    @Test
    void should_return_0_when_vectors_are_orthogonal() {
        float[] a = {1.0f, 0.0f};
        float[] b = {0.0f, 1.0f};
        assertThat(CosineSimilarity.compute(a, b)).isCloseTo(0.0, within(1e-6));
    }

    @Test
    void should_return_expected_when_partial_similarity() {
        // 45° angle → cos(45°) = √2/2 ≈ 0.707
        float[] a = {1.0f, 0.0f};
        float[] b = {1.0f, 1.0f};
        assertThat(CosineSimilarity.compute(a, b)).isCloseTo(Math.sqrt(2) / 2, within(1e-6));
    }

    @Test
    void should_return_negative_when_opposite_directions() {
        float[] a = {1.0f, 0.0f};
        float[] b = {-1.0f, 0.0f};
        assertThat(CosineSimilarity.compute(a, b)).isCloseTo(-1.0, within(1e-6));
    }

    @Test
    void should_be_symmetric() {
        float[] a = {0.3f, 0.7f, 0.5f};
        float[] b = {0.1f, 0.9f, 0.2f};
        assertThat(CosineSimilarity.compute(a, b))
                .isCloseTo(CosineSimilarity.compute(b, a), within(1e-6));
    }

    @Test
    void should_throw_when_vectors_have_different_dimensions() {
        float[] a = {1.0f, 0.0f};
        float[] b = {1.0f, 0.0f, 0.5f};
        assertThatThrownBy(() -> CosineSimilarity.compute(a, b))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dimension");
    }

    @Test
    void should_throw_when_vector_is_null() {
        assertThatThrownBy(() -> CosineSimilarity.compute(null, new float[]{1.0f}))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throw_when_vector_is_zero() {
        float[] zero = {0.0f, 0.0f};
        float[] other = {1.0f, 0.0f};
        assertThatThrownBy(() -> CosineSimilarity.compute(zero, other))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("zero");
    }
}
