package com.quizapp.persistence.jdbc;

import com.quizapp.attempt.AttemptRepository;
import com.quizapp.domain.Attempt;

import java.util.List;
import java.util.Optional;

/**
 * Поки що заглушка — тести JdbcRepositoryContractTest мають падати.
 */
public class JdbcAttemptRepository implements AttemptRepository {

    private final Database database;

    public JdbcAttemptRepository(Database database) {
        this.database = database;
    }

    @Override
    public Attempt save(Attempt attempt) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }

    @Override
    public Optional<Attempt> findById(long id) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }

    @Override
    public List<Attempt> findCompletedByUser(long userId) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }
}
