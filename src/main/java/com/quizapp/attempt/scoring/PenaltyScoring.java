package com.quizapp.attempt.scoring;

import java.util.List;

/**
 * Оцінювання зі штрафом: за правильну відповідь — бали запитання,
 * за неправильну — мінус чверть балів запитання, за пропущену — 0.
 * Підсумок не може бути меншим за 0.
 */
public class PenaltyScoring implements ScoringStrategy {

    @Override
    public ScoringResult score(List<GivenAnswer> answers) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }
}
