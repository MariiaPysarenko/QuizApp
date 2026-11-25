package com.quizapp.attempt.scoring;

/**
 * Звичайне оцінювання: за правильну відповідь — бали запитання,
 * за неправильну або пропущену — 0.
 */
public class WeightedScoring extends AbstractScoringStrategy {

    @Override
    protected double pointsFor(GivenAnswer answer) {
        return answer.isCorrect() ? answer.question().points() : 0;
    }
}
