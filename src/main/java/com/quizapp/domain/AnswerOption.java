package com.quizapp.domain;

/**
 * Варіант відповіді на запитання.
 *
 * @param id ідентифікатор; {@code null}, доки варіант не збережено
 */
public record AnswerOption(Long id, String text, boolean correct) {
}
