package com.quizapp.domain;

import java.util.List;

/**
 * Запитання квізу: текст, кількість балів, необов'язкове пояснення та варіанти відповіді.
 *
 * @param id ідентифікатор; {@code null}, доки запитання не збережено
 */
public record Question(Long id, String text, int points, String explanation, List<AnswerOption> options) {

    public Question {
        options = List.copyOf(options);
    }
}
