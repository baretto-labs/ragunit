package org.ragunit.core.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests for the TestCase and Testset domain records. */
class TestCaseTest {

    private static final Question QUESTION = new Question("What is the capital of France?");
    private static final ReferenceAnswer REFERENCE = new ReferenceAnswer("Paris.");
    private static final Document DOC = new Document("Paris is the capital of France.");
    private static final List<Document> CONTEXT = List.of(DOC);

    @Test
    void should_createTestCase_when_allFieldsAreValid() {
        TestCase tc = TestCase.simple(QUESTION, CONTEXT, REFERENCE);
        assertThat(tc.question()).isEqualTo(QUESTION);
        assertThat(tc.context()).isEqualTo(CONTEXT);
        assertThat(tc.referenceAnswer()).isEqualTo(REFERENCE);
        assertThat(tc.questionType()).isEqualTo(QuestionType.SIMPLE);
    }

    @Test
    void should_createMultiHopTestCase_when_usingMultiHopFactory() {
        TestCase tc = TestCase.multiHop(QUESTION, CONTEXT, REFERENCE);
        assertThat(tc.questionType()).isEqualTo(QuestionType.MULTI_HOP);
    }

    @Test
    void should_rejectNullQuestion_when_creatingTestCase() {
        assertThatThrownBy(() -> new TestCase(null, CONTEXT, REFERENCE, QuestionType.SIMPLE))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_rejectNullContext_when_creatingTestCase() {
        assertThatThrownBy(() -> new TestCase(QUESTION, null, REFERENCE, QuestionType.SIMPLE))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_rejectNullReferenceAnswer_when_creatingTestCase() {
        assertThatThrownBy(() -> new TestCase(QUESTION, CONTEXT, null, QuestionType.SIMPLE))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_rejectNullQuestionType_when_creatingTestCase() {
        assertThatThrownBy(() -> new TestCase(QUESTION, CONTEXT, REFERENCE, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_makeContextImmutable_when_creatingTestCase() {
        var mutable = new java.util.ArrayList<>(CONTEXT);
        TestCase tc = TestCase.simple(QUESTION, mutable, REFERENCE);
        mutable.add(new Document("extra"));
        assertThat(tc.context()).hasSize(1);
    }

    // --- Testset ---

    @Test
    void should_createTestset_when_casesAreValid() {
        TestCase tc = TestCase.simple(QUESTION, CONTEXT, REFERENCE);
        Testset testset = new Testset(List.of(tc));
        assertThat(testset.size()).isEqualTo(1);
        assertThat(testset.cases()).containsExactly(tc);
    }

    @Test
    void should_rejectNullCases_when_creatingTestset() {
        assertThatThrownBy(() -> new Testset(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_returnZeroSize_when_testsetIsEmpty() {
        Testset testset = new Testset(List.of());
        assertThat(testset.size()).isEqualTo(0);
        assertThat(testset.isEmpty()).isTrue();
    }

    @Test
    void should_returnFalseForEmpty_when_testsetHasCases() {
        Testset testset = new Testset(List.of(TestCase.simple(QUESTION, CONTEXT, REFERENCE)));
        assertThat(testset.isEmpty()).isFalse();
    }
}
