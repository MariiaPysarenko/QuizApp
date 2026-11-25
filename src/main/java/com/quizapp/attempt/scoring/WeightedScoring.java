package com.quizapp.attempt.scoring;

import java.util.List;

/**
 * Звичайне оцінювання: за правильну відповідь — бали запитання,
 * за неправильну або пропущену — 0.
 */
public class WeightedScoring implements ScoringStrategy {

    @Override
    public ScoringResult score(List<GivenAnswer> answers) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }
}
