package com.quizapp.domain;

import java.util.List;

/**
 * Запитання квізу: текст, кількість балів, необов'язкове пояснення та варіанти відповіді.
 */
public record Question(String text, int points, String explanation, List<AnswerOption> options) {
}
