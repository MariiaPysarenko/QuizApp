package com.quizapp.attempt.scoring;

import java.util.ArrayList;
import java.util.List;

/**
 * Спільний алгоритм оцінювання для всіх стратегій (патерн Template Method).
 * <p>
 * Обхід відповідей, підрахунок максимуму та збирання результату однакові,
 * тому вони зафіксовані тут у {@code final}-методі. Підкласи визначають лише,
 * скільки балів дати за конкретну відповідь, і, за потреби, як скоригувати підсумок.
 */
public abstract class AbstractScoringStrategy implements ScoringStrategy {

    @Override
    public final ScoringResult score(List<GivenAnswer> answers) {
        List<QuestionResult> results = new ArrayList<>();
        double score = 0;
        double maxScore = 0;
        for (GivenAnswer answer : answers) {
            double points = pointsFor(answer);
            results.add(new QuestionResult(answer.question(), answer.selected(), answer.isCorrect(), points));
            score += points;
            maxScore += answer.question().points();
        }
        return new ScoringResult(results, adjustTotal(score), maxScore);
    }

    /** Бали за одну відповідь. */
    protected abstract double pointsFor(GivenAnswer answer);

    /** Коригування підсумку; за замовчуванням без змін. */
    protected double adjustTotal(double total) {
        return total;
    }
}
