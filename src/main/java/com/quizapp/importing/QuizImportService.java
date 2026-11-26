package com.quizapp.importing;

import com.quizapp.common.ValidationException;
import com.quizapp.quiz.QuizRepository;

import java.util.stream.Collectors;

/**
 * Імпорт квізу з будь-якого джерела {@link QuizImport}.
 * Сервіс не знає, звідки прийшов квіз: Google Forms чи інший формат.
 */
public class QuizImportService {

    private final QuizRepository quizzes;

    public QuizImportService(QuizRepository quizzes) {
        this.quizzes = quizzes;
    }

    public ImportResult importQuiz(QuizImport source, long teacherId) {
        ImportResult result = source.toQuiz(teacherId);
        if (!result.hasQuiz()) {
            String reasons = result.skipped().stream()
                    .map(item -> item.position() + ". " + item.title() + " — " + item.reason())
                    .collect(Collectors.joining("; "));
            throw new ValidationException("Не вдалося імпортувати жодного запитання"
                    + (reasons.isEmpty() ? "" : ": " + reasons));
        }
        return result.withQuiz(quizzes.save(result.quiz()));
    }
}
