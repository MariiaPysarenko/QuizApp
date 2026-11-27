package com.quizapp.web.dto;

import java.util.List;

/** Добірка запитань для роботи над помилками (без правильних відповідей). */
public record PracticeView(List<QuizView.QuestionView> questions) {
}
