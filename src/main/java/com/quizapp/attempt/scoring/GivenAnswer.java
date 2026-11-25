package com.quizapp.attempt.scoring;

import com.quizapp.domain.AnswerOption;
import com.quizapp.domain.Question;

/**
 * Відповідь студента на одне запитання.
 *
 * @param question запитання
 * @param selected обраний варіант або {@code null}, якщо запитання пропущено
 */
public record GivenAnswer(Question question, AnswerOption selected) {

    public GivenAnswer {
        if (question == null) {
            throw new IllegalArgumentException("Запитання не може бути null");
        }
        if (selected != null && !question.options().contains(selected)) {
            throw new IllegalArgumentException("Обраний варіант не належить до запитання");
        }
    }

    public boolean isSkipped() {
        return selected == null;
    }

    public boolean isCorrect() {
        return selected != null && selected.correct();
    }
}
