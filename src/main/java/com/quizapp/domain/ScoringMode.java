package com.quizapp.domain;

/**
 * Спосіб підрахунку балів за квіз.
 */
public enum ScoringMode {
    /** Сума балів за правильні відповіді. */
    WEIGHTED,
    /** Як WEIGHTED, але неправильна відповідь знімає чверть балів запитання. */
    PENALTY
}
