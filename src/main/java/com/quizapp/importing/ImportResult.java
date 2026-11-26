package com.quizapp.importing;

import com.quizapp.domain.Quiz;

import java.util.List;

/**
 * Результат імпорту: квіз (або {@code null}, якщо не імпортовано жодного запитання)
 * і звіт про пропущені запитання.
 */
public record ImportResult(Quiz quiz, List<SkippedItem> skipped) {

    public boolean hasQuiz() {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }

    public int importedCount() {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }
}
