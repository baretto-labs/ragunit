package org.ragunit.core.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests for Document: content preservation and null guard. */
class DocumentTest {

    private static final String CONTENT = "Paris is the capital of France.";

    @Test
    void should_preserveContent_when_created() {
        assertThat(new Document(CONTENT).content()).isEqualTo(CONTENT);
    }

    @Test
    void should_rejectDocument_when_contentIsNull() {
        assertThatThrownBy(() -> new Document(null))
                .isInstanceOf(NullPointerException.class);
    }
}
