package org.ragunit.core.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests for Answer: text preservation and null guard. */
class AnswerTest {

    private static final String TEXT = "Paris is the capital of France.";

    @Test
    void should_preserveText_when_created() {
        assertThat(new Answer(TEXT).text()).isEqualTo(TEXT);
    }

    @Test
    void should_rejectAnswer_when_textIsNull() {
        assertThatThrownBy(() -> new Answer(null))
                .isInstanceOf(NullPointerException.class);
    }
}
