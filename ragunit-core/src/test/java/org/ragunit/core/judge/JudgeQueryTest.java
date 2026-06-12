package org.ragunit.core.judge;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link JudgeQuery}: the generic evaluation request — a criterion
 * plus arbitrary named inputs — that decouples assertions from RAG-shaped APIs.
 */
class JudgeQueryTest {

    private static final Criterion CONCISENESS =
            Criterion.of("conciseness", "Is the summary concise and faithful to the source?");

    @Test
    void should_exposeCriterionAndInputs_when_builtViaBuilder() {
        JudgeQuery query = JudgeQuery.builder()
                .criterion(CONCISENESS)
                .input("source", "A long article about France.")
                .input("summary", "France, briefly.")
                .build();

        assertThat(query.criterion().name()).isEqualTo("conciseness");
        assertThat(query.firstInput("source")).contains("A long article about France.");
        assertThat(query.firstInput("summary")).contains("France, briefly.");
    }

    @Test
    void should_supportMultiValueInputs_when_inputIsAList() {
        JudgeQuery query = JudgeQuery.builder()
                .criterion(CONCISENESS)
                .input("context", List.of("chunk one", "chunk two"))
                .build();

        assertThat(query.inputValues("context")).containsExactly("chunk one", "chunk two");
    }

    @Test
    void should_exposeAllInputsInInsertionOrder_when_readingInputsMap() {
        JudgeQuery query = JudgeQuery.builder()
                .criterion(CONCISENESS)
                .input("source", "article")
                .input("summary", "short")
                .build();

        assertThat(query.inputs().keySet()).containsExactly("source", "summary");
        assertThat(query.inputs().get("summary")).containsExactly("short");
    }

    @Test
    void should_returnEmptyList_when_inputNameIsUnknown() {
        JudgeQuery query = JudgeQuery.builder()
                .criterion(CONCISENESS)
                .input("summary", "text")
                .build();

        assertThat(query.inputValues("unknown")).isEmpty();
        assertThat(query.firstInput("unknown")).isEmpty();
    }

    @Test
    void should_renderInstructionAndInputs_when_renderingPromptBody() {
        JudgeQuery query = JudgeQuery.builder()
                .criterion(CONCISENESS)
                .input("Source", "A long article about France.")
                .input("Summary", "France, briefly.")
                .build();

        String rendered = query.render();

        assertThat(rendered)
                .contains("Is the summary concise and faithful to the source?")
                .contains("Source: A long article about France.")
                .contains("Summary: France, briefly.");
    }

    @Test
    void should_renderMultiValueInputAsBulletList_when_rendering() {
        JudgeQuery query = JudgeQuery.builder()
                .criterion(CONCISENESS)
                .input("Context", List.of("chunk one", "chunk two"))
                .build();

        assertThat(query.render())
                .contains("- chunk one")
                .contains("- chunk two");
    }

    @Test
    void should_preserveInputOrder_when_rendering() {
        JudgeQuery query = JudgeQuery.builder()
                .criterion(CONCISENESS)
                .input("First", "1")
                .input("Second", "2")
                .build();

        String rendered = query.render();

        assertThat(rendered.indexOf("First")).isLessThan(rendered.indexOf("Second"));
    }

    @Test
    void should_throwIllegalStateException_when_builtWithoutCriterion() {
        assertThatThrownBy(() -> JudgeQuery.builder().input("a", "b").build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("criterion");
    }

    @Test
    void should_throwIllegalStateException_when_builtWithoutAnyInput() {
        assertThatThrownBy(() -> JudgeQuery.builder().criterion(CONCISENESS).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("input");
    }

    @Test
    void should_exposeRequiredInput_when_present() {
        JudgeQuery query = JudgeQuery.builder()
                .criterion(CONCISENESS)
                .input("question", "What?")
                .build();

        assertThat(query.requiredInput("question")).isEqualTo("What?");
    }

    @Test
    void should_throwJudgeException_when_requiredInputIsMissing() {
        JudgeQuery query = JudgeQuery.builder()
                .criterion(CONCISENESS)
                .input("answer", "text")
                .build();

        assertThatThrownBy(() -> query.requiredInput("question"))
                .isInstanceOf(JudgeException.class)
                .hasMessageContaining("question");
    }
}
