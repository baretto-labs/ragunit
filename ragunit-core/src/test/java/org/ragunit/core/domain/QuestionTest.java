package org.ragunit.core.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests for Question: text preservation, null guard, and blank guard. */
class QuestionTest {

    private static final String TEXT = "What is the capital of France?";

    @Test
    void should_preserveText_when_created() {
        assertThat(new Question(TEXT).text()).isEqualTo(TEXT);
    }

    @Test
    void should_rejectQuestion_when_textIsNull() {
        assertThatThrownBy(() -> new Question(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_rejectQuestion_when_textIsBlank() {
        assertThatThrownBy(() -> new Question("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
