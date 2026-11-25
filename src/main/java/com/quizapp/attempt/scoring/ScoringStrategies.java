package com.quizapp.attempt.scoring;

import com.quizapp.domain.ScoringMode;

/**
 * Вибір стратегії оцінювання за налаштуванням квізу.
 * <p>
 * Квіз зберігає лише значення {@link ScoringMode}, а не клас стратегії, —
 * так доменна модель не залежить від реалізацій оцінювання.
 */
public final class ScoringStrategies {

    private static final ScoringStrategy WEIGHTED = new WeightedScoring();
    private static final ScoringStrategy PENALTY = new PenaltyScoring();

    private ScoringStrategies() {
    }

    public static ScoringStrategy forMode(ScoringMode mode) {
        return switch (mode) {
            case WEIGHTED -> WEIGHTED;
            case PENALTY -> PENALTY;
        };
    }
}
