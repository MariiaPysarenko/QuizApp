package com.quizapp.importing;

import com.quizapp.quiz.QuizRepository;

/**
 * Імпорт квізу з будь-якого джерела {@link QuizImport}.
 */
public class QuizImportService {

    private final QuizRepository quizzes;

    public QuizImportService(QuizRepository quizzes) {
        this.quizzes = quizzes;
    }

    public ImportResult importQuiz(QuizImport source, long teacherId) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }
}
