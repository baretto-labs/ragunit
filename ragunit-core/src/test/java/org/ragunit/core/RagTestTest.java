package org.ragunit.core;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies that @RagTest is a valid JUnit 5 meta-annotation with the rag-eval tag. */
class RagTestTest {

    @Test
    void should_beATestMethod_when_annotationIsPresent() {
        assertThat(RagTest.class.isAnnotationPresent(Test.class)).isTrue();
    }

    @Test
    void should_beTaggedWithRagEval_when_annotationIsPresent() {
        Tag tag = RagTest.class.getAnnotation(Tag.class);
        assertThat(tag).isNotNull();
        assertThat(tag.value()).isEqualTo("rag-eval");
    }
}
