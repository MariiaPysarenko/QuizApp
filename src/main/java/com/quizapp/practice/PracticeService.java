package com.quizapp.practice;

import com.quizapp.attempt.AttemptRepository;
import com.quizapp.attempt.scoring.ScoringResult;
import com.quizapp.domain.Question;
import com.quizapp.quiz.QuizRepository;

import java.time.Clock;
import java.util.List;
import java.util.Map;

/**
 * Робота над помилками: повторне проходження запитань, на які студент востаннє відповів неправильно.
 */
public class PracticeService {

    private final QuizRepository quizzes;
    private final AttemptRepository attempts;
    private final Clock clock;

    public PracticeService(QuizRepository quizzes, AttemptRepository attempts, Clock clock) {
        this.quizzes = quizzes;
        this.attempts = attempts;
        this.clock = clock;
    }

    public List<Question> buildPractice(long studentId) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }

    public ScoringResult submit(long studentId, Map<Long, Long> selectedOptions) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }
}
