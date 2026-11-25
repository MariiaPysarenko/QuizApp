package com.quizapp.attempt.scoring;

import com.quizapp.domain.ScoringMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScoringStrategiesTest {

    @Test
    @DisplayName("Кожному способу оцінювання відповідає своя стратегія")
    void returnsStrategyForEachMode() {
        assertThat(ScoringStrategies.forMode(ScoringMode.WEIGHTED)).isInstanceOf(WeightedScoring.class);
        assertThat(ScoringStrategies.forMode(ScoringMode.PENALTY)).isInstanceOf(PenaltyScoring.class);
    }
}
