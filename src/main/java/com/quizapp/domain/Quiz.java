package com.quizapp.domain;

import java.time.Duration;
import java.util.List;

/**
 * Квіз — набір запитань зі спільною темою.
 * Створюється через {@link com.quizapp.quiz.QuizBuilder}, який перевіряє коректність даних.
 *
 * @param id          ідентифікатор; {@code null}, доки квіз не збережено
 * @param creatorId   автор квізу (викладач)
 * @param scoringMode спосіб підрахунку балів
 * @param timeLimit   обмеження часу; {@code null} — без обмеження
 * @param status      чернетка, опублікований або архівний
 */
public record Quiz(Long id,
                   Long creatorId,
                   String title,
                   String description,
                   ScoringMode scoringMode,
                   Duration timeLimit,
                   QuizStatus status,
                   List<Question> questions) {

    public Quiz {
        questions = List.copyOf(questions);
    }

    public boolean isPublished() {
        return status == QuizStatus.PUBLISHED;
    }
}
