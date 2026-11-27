package com.quizapp.web.dto;

import com.quizapp.attempt.scoring.QuestionResult;
import com.quizapp.attempt.scoring.ScoringResult;
import com.quizapp.domain.AnswerOption;

import java.util.List;

/** Результат оцінювання: бали, відсоток і розбір кожного запитання. */
public record ResultView(double score, double maxScore, double percent, List<QuestionResultView> questions) {

    public record QuestionResultView(long questionId,
                                     String text,
                                     Long selectedOptionId,
                                     long correctOptionId,
                                     boolean correct,
                                     double points,
                                     String explanation) {
    }

    public static ResultView from(ScoringResult result) {
        List<QuestionResultView> questions = result.questions().stream()
                .map(ResultView::question)
                .toList();
        return new ResultView(result.score(), result.maxScore(), round(result.percent()), questions);
    }

    static double round(double percent) {
        return Math.round(percent * 10) / 10.0;
    }

    private static QuestionResultView question(QuestionResult result) {
        long correctOptionId = result.question().options().stream()
                .filter(AnswerOption::correct)
                .findFirst()
                .orElseThrow()
                .id();
        return new QuestionResultView(
                result.question().id(),
                result.question().text(),
                result.selected() == null ? null : result.selected().id(),
                correctOptionId,
                result.correct(),
                result.points(),
                result.question().explanation());
    }
}
