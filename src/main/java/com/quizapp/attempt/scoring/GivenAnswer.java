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

    public boolean isSkipped() {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }

    public boolean isCorrect() {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }
}
