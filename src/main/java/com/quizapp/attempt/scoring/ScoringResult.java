package com.quizapp.attempt.scoring;

import java.util.List;

/**
 * Підсумок оцінювання спроби.
 *
 * @param questions результати по кожному запитанню
 * @param score     набрані бали
 * @param maxScore  максимально можливі бали
 */
public record ScoringResult(List<QuestionResult> questions, double score, double maxScore) {

    /** Відсоток від максимуму; 0, якщо запитань немає. */
    public double percent() {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }

    /** Запитання з неправильною або пропущеною відповіддю — основа для роботи над помилками. */
    public List<QuestionResult> mistakes() {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }
}
