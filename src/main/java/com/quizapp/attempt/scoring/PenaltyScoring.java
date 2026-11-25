package com.quizapp.attempt.scoring;

import java.util.ArrayList;
import java.util.List;

/**
 * Оцінювання зі штрафом: за правильну відповідь — бали запитання,
 * за неправильну — мінус чверть балів запитання, за пропущену — 0.
 * Підсумок не може бути меншим за 0.
 * <p>
 * Штраф відбиває бажання вгадувати: пропустити запитання вигідніше, ніж відповісти навмання.
 */
public class PenaltyScoring implements ScoringStrategy {

    static final double PENALTY_RATE = 0.25;

    @Override
    public ScoringResult score(List<GivenAnswer> answers) {
        List<QuestionResult> results = new ArrayList<>();
        double score = 0;
        double maxScore = 0;
        for (GivenAnswer answer : answers) {
            int questionPoints = answer.question().points();
            double points;
            if (answer.isCorrect()) {
                points = questionPoints;
            } else if (answer.isSkipped()) {
                points = 0;
            } else {
                points = -questionPoints * PENALTY_RATE;
            }
            results.add(new QuestionResult(answer.question(), answer.selected(), answer.isCorrect(), points));
            score += points;
            maxScore += questionPoints;
        }
        return new ScoringResult(results, Math.max(0, score), maxScore);
    }
}
