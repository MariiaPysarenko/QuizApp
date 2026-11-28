package com.quizapp.persistence.jdbc;

import com.quizapp.domain.Question;
import com.quizapp.domain.Quiz;
import com.quizapp.quiz.QuizRepository;

import java.util.List;
import java.util.Optional;

/**
 * Поки що заглушка — тести JdbcRepositoryContractTest мають падати.
 */
public class JdbcQuizRepository implements QuizRepository {

    private final Database database;

    public JdbcQuizRepository(Database database) {
        this.database = database;
    }

    @Override
    public Quiz save(Quiz quiz) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }

    @Override
    public Optional<Quiz> findById(long id) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }

    @Override
    public List<Quiz> findAll() {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }

    @Override
    public Optional<Question> findQuestionById(long questionId) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }
}
