package com.quizapp.importing;

import com.quizapp.domain.Quiz;

import java.util.List;

/**
 * Результат імпорту: квіз (або {@code null}, якщо не імпортовано жодного запитання)
 * і звіт про пропущені запитання.
 */
public record ImportResult(Quiz quiz, List<SkippedItem> skipped) {

    public ImportResult {
        skipped = List.copyOf(skipped);
    }

    public boolean hasQuiz() {
        return quiz != null;
    }

    public int importedCount() {
        return quiz == null ? 0 : quiz.questions().size();
    }
}
