package com.quizapp.web.dto;

import com.quizapp.importing.ImportResult;
import com.quizapp.importing.SkippedItem;

import java.util.List;

/** Результат імпорту: створений квіз і звіт про пропущені запитання. */
public record ImportView(QuizSummaryView quiz, int importedCount, List<SkippedItem> skipped) {

    public static ImportView from(ImportResult result) {
        return new ImportView(QuizSummaryView.from(result.quiz()), result.importedCount(), result.skipped());
    }
}
