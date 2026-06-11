package org.ragunit.core.assertion;

import org.junit.jupiter.api.Test;
import org.ragunit.core.domain.Answer;
import org.ragunit.core.domain.FactualCorrectnessVerdict;
import org.ragunit.core.domain.Question;
import org.ragunit.core.domain.ReferenceAnswer;
import org.ragunit.core.domain.Score;
import org.ragunit.core.report.AssertionResult;
import org.ragunit.core.report.RagReporter;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests for FactualCorrectness assertions on {@link AnswerAssert}. */
class AnswerAssertFactualCorrectnessTest {

    private static final double THRESHOLD = 0.75;
    private static final Question QUESTION = new Question("Who discovered penicillin?");
    private static final Answer ANSWER = new Answer("Alexander Fleming discovered penicillin in 1928.");
    private static final ReferenceAnswer REFERENCE = new ReferenceAnswer(
            "Alexander Fleming discovered penicillin in 1928 while working at St Mary's Hospital.");

    private final List<AssertionResult> captured = new ArrayList<>();
    private final List<RagReporter> reporters = List.of(captured::add);

    private static FactualCorrectnessVerdict passingVerdict() {
        return new FactualCorrectnessVerdict(
                new Score(0.90), new Score(0.95), new Score(0.85), "Strong overlap.", "stub");
    }

    private static FactualCorrectnessVerdict lowF1Verdict() {
        return new FactualCorrectnessVerdict(
                new Score(0.50), new Score(0.90), new Score(0.33), "Low recall.", "stub");
    }

    private static FactualCorrectnessVerdict lowPrecisionVerdict() {
        return new FactualCorrectnessVerdict(
                new Score(0.60), new Score(0.40), new Score(0.90), "Low precision.", "stub");
    }

    private static FactualCorrectnessVerdict lowRecallVerdict() {
        return new FactualCorrectnessVerdict(
                new Score(0.55), new Score(0.90), new Score(0.38), "Low recall.", "stub");
    }

    @Test
    void should_pass_when_f1_meetsThreshold() {
        new AnswerAssert(ANSWER, reporters)
                .forQuestion(QUESTION)
                .comparedTo(REFERENCE)
                .evaluatedBy(new StubRagJudgeWithFactual(passingVerdict()))
                .hasFactualCorrectnessF1(THRESHOLD);
    }

    @Test
    void should_fail_when_f1_belowThreshold() {
        assertThatThrownBy(() ->
                new AnswerAssert(ANSWER, reporters)
                        .forQuestion(QUESTION)
                        .comparedTo(REFERENCE)
                        .evaluatedBy(new StubRagJudgeWithFactual(lowF1Verdict()))
                        .hasFactualCorrectnessF1(THRESHOLD)
        ).isInstanceOf(AssertionError.class)
                .hasMessageContaining("Factual correctness F1");
    }

    @Test
    void should_fail_when_precision_belowThreshold() {
        assertThatThrownBy(() ->
                new AnswerAssert(ANSWER, reporters)
                        .forQuestion(QUESTION)
                        .comparedTo(REFERENCE)
                        .evaluatedBy(new StubRagJudgeWithFactual(lowPrecisionVerdict()))
                        .hasFactualCorrectnessPrecision(THRESHOLD)
        ).isInstanceOf(AssertionError.class)
                .hasMessageContaining("Factual correctness precision");
    }

    @Test
    void should_fail_when_recall_belowThreshold() {
        assertThatThrownBy(() ->
                new AnswerAssert(ANSWER, reporters)
                        .forQuestion(QUESTION)
                        .comparedTo(REFERENCE)
                        .evaluatedBy(new StubRagJudgeWithFactual(lowRecallVerdict()))
                        .hasFactualCorrectnessRecall(THRESHOLD)
        ).isInstanceOf(AssertionError.class)
                .hasMessageContaining("Factual correctness recall");
    }

    @Test
    void should_reportResult_when_assertionRuns() {
        new AnswerAssert(ANSWER, reporters)
                .forQuestion(QUESTION)
                .comparedTo(REFERENCE)
                .evaluatedBy(new StubRagJudgeWithFactual(passingVerdict()))
                .hasFactualCorrectnessF1(THRESHOLD);

        assertThat(captured).hasSize(1);
        assertThat(captured.get(0).assertionType()).isEqualTo("FACTUAL_CORRECTNESS");
    }

    @Test
    void should_throwNPE_when_comparedToCalledWithNull() {
        assertThatThrownBy(() ->
                new AnswerAssert(ANSWER, reporters).comparedTo(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_chain_f1_and_precision_assertions() {
        new AnswerAssert(ANSWER, reporters)
                .forQuestion(QUESTION)
                .comparedTo(REFERENCE)
                .evaluatedBy(new StubRagJudgeWithFactual(passingVerdict()))
                .hasFactualCorrectnessF1(0.80)
                .hasFactualCorrectnessPrecision(0.90)
                .hasFactualCorrectnessRecall(0.80);
    }
}
