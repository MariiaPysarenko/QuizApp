package com.quizapp.quiz;

import com.quizapp.domain.Quiz;
import com.quizapp.domain.User;

import java.util.List;
import java.util.Optional;

/**
 * Керування квізами: створення, перегляд з урахуванням ролі, публікація.
 * Поки що заглушка — тести QuizServiceTest мають падати.
 */
public class QuizService {

    private final QuizRepository quizzes;

    public QuizService(QuizRepository quizzes) {
        this.quizzes = quizzes;
    }

    public Quiz create(long teacherId, QuizBuilder builder) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }

    public List<Quiz> list(User user) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }

    public Quiz get(User user, long quizId) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }

    public Optional<Quiz> find(long quizId) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }

    public Quiz publish(long teacherId, long quizId) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }
}
