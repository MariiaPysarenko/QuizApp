package com.quizapp.attempt;

import com.quizapp.attempt.scoring.ScoringResult;
import com.quizapp.domain.Attempt;
import com.quizapp.quiz.QuizRepository;

import java.time.Clock;
import java.util.Map;

/**
 * Проходження квізу: початок спроби, перевірка та оцінювання відповідей.
 */
public class AttemptService {

    private final QuizRepository quizzes;
    private final AttemptRepository attempts;
    private final Clock clock;

    public AttemptService(QuizRepository quizzes, AttemptRepository attempts, Clock clock) {
        this.quizzes = quizzes;
        this.attempts = attempts;
        this.clock = clock;
    }

    public Attempt start(long studentId, long quizId) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }

    public ScoringResult submit(long studentId, long attemptId, Map<Long, Long> selectedOptions) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }
}
