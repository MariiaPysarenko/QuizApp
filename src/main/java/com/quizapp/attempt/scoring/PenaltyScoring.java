package com.quizapp.attempt.scoring;

/**
 * Оцінювання зі штрафом: за правильну відповідь — бали запитання,
 * за неправильну — мінус чверть балів запитання, за пропущену — 0.
 * Підсумок не може бути меншим за 0.
 * <p>
 * Штраф відбиває бажання вгадувати: пропустити запитання вигідніше, ніж відповісти навмання.
 */
public class PenaltyScoring extends AbstractScoringStrategy {

    static final double PENALTY_RATE = 0.25;

    @Override
    protected double pointsFor(GivenAnswer answer) {
        if (answer.isCorrect()) {
            return answer.question().points();
        }
        if (answer.isSkipped()) {
            return 0;
        }
        return -answer.question().points() * PENALTY_RATE;
    }

    @Override
    protected double adjustTotal(double total) {
        return Math.max(0, total);
    }
}
