package com.quizapp.attempt.scoring;

import java.util.ArrayList;
import java.util.List;

/**
 * Звичайне оцінювання: за правильну відповідь — бали запитання,
 * за неправильну або пропущену — 0.
 */
public class WeightedScoring implements ScoringStrategy {

    @Override
    public ScoringResult score(List<GivenAnswer> answers) {
        List<QuestionResult> results = new ArrayList<>();
        double score = 0;
        double maxScore = 0;
        for (GivenAnswer answer : answers) {
            int questionPoints = answer.question().points();
            double points = answer.isCorrect() ? questionPoints : 0;
            results.add(new QuestionResult(answer.question(), answer.selected(), answer.isCorrect(), points));
            score += points;
            maxScore += questionPoints;
        }
        return new ScoringResult(results, score, maxScore);
    }
}
