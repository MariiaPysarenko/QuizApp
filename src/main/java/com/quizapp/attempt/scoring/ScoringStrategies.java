package com.quizapp.attempt.scoring;

import com.quizapp.domain.ScoringMode;

/**
 * Вибір стратегії оцінювання за налаштуванням квізу.
 */
public final class ScoringStrategies {

    private ScoringStrategies() {
    }

    public static ScoringStrategy forMode(ScoringMode mode) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }
}
