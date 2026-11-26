package com.quizapp.importing.googleforms;

import com.quizapp.importing.ImportResult;
import com.quizapp.importing.QuizImport;

/**
 * Патерн Adapter (структурний): пристосовує експорт Google Forms до інтерфейсу {@link QuizImport}.
 * Поки що заглушка — тести GoogleFormsQuizAdapterTest мають падати.
 */
public class GoogleFormsQuizAdapter implements QuizImport {

    private final GoogleFormExport export;

    public GoogleFormsQuizAdapter(GoogleFormExport export) {
        this.export = export;
    }

    @Override
    public ImportResult toQuiz(long creatorId) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }
}
