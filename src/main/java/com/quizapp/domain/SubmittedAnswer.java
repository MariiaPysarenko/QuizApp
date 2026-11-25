package com.quizapp.domain;

/**
 * Збережена відповідь студента на одне запитання.
 *
 * @param selectedOptionId обраний варіант або {@code null}, якщо запитання пропущено
 */
public record SubmittedAnswer(long questionId, Long selectedOptionId, boolean correct) {
}
