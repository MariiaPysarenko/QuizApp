package com.quizapp.domain;

import java.util.List;

/**
 * Квіз — набір запитань зі спільною темою.
 * Створюється через {@link com.quizapp.quiz.QuizBuilder}, який перевіряє коректність даних.
 */
public record Quiz(String title, String description, List<Question> questions) {
}
