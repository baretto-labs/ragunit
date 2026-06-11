package org.ragunit.core.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests for {@link ScoreStatistics}: mean and population standard deviation
 * over repeated judge runs.
 */
class ScoreStatisticsTest {

    @Test
    void should_computeMean_when_aggregatingMultipleScores() {
        ScoreStatistics stats = ScoreStatistics.of(scores(0.9, 0.5));

        assertThat(stats.mean()).isEqualTo(0.7, within(1e-9));
    }

    @Test
    void should_computePopulationStddev_when_aggregatingMultipleScores() {
        ScoreStatistics stats = ScoreStatistics.of(scores(0.9, 0.5));

        assertThat(stats.stddev()).isEqualTo(0.2, within(1e-9));
    }

    @Test
    void should_haveZeroStddev_when_singleRun() {
        ScoreStatistics stats = ScoreStatistics.of(scores(0.8));

        assertThat(stats.stddev()).isEqualTo(0.0);
        assertThat(stats.runs()).isEqualTo(1);
    }

    @Test
    void should_haveZeroStddev_when_allScoresIdentical() {
        ScoreStatistics stats = ScoreStatistics.of(scores(0.7, 0.7, 0.7));

        assertThat(stats.stddev()).isEqualTo(0.0, within(1e-9));
    }

    @Test
    void should_exposeRunCount_when_aggregating() {
        ScoreStatistics stats = ScoreStatistics.of(scores(0.9, 0.8, 0.7));

        assertThat(stats.runs()).isEqualTo(3);
    }

    @Test
    void should_throwIllegalArgumentException_when_scoresListIsEmpty() {
        assertThatThrownBy(() -> ScoreStatistics.of(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reportBelowThreshold_when_meanIsLower() {
        ScoreStatistics stats = ScoreStatistics.of(scores(0.5, 0.5));

        assertThat(stats.isBelow(0.6)).isTrue();
        assertThat(stats.isBelow(0.4)).isFalse();
    }

    @Test
    void should_reportUnstable_when_stddevExceedsBound() {
        ScoreStatistics stats = ScoreStatistics.of(scores(0.9, 0.5));

        assertThat(stats.isUnstable(0.15)).isTrue();
        assertThat(stats.isUnstable(0.25)).isFalse();
    }

    @Test
    void should_meetThresholdExactly_when_meanEqualsThreshold() {
        ScoreStatistics stats = ScoreStatistics.of(scores(0.8, 0.8));

        assertThat(stats.isBelow(0.8)).isFalse();
    }

    private static List<Score> scores(double... values) {
        return java.util.Arrays.stream(values).mapToObj(Score::new).toList();
    }
}
