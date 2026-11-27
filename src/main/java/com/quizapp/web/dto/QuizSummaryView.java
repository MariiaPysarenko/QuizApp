package com.quizapp.web.dto;

import com.quizapp.domain.Quiz;
import com.quizapp.domain.QuizStatus;
import com.quizapp.domain.ScoringMode;

/** Короткі відомості про квіз для списку. */
public record QuizSummaryView(long id,
                              String title,
                              String description,
                              QuizStatus status,
                              ScoringMode scoringMode,
                              Long timeLimitSeconds,
                              int questionCount) {

    public static QuizSummaryView from(Quiz quiz) {
        return new QuizSummaryView(quiz.id(), quiz.title(), quiz.description(), quiz.status(),
                quiz.scoringMode(), QuizView.seconds(quiz), quiz.questions().size());
    }
}
