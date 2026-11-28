package com.quizapp.persistence.jdbc;

import com.quizapp.auth.UserRepository;
import com.quizapp.domain.User;

import java.util.Optional;

/**
 * Поки що заглушка — тести JdbcRepositoryContractTest мають падати.
 */
public class JdbcUserRepository implements UserRepository {

    private final Database database;

    public JdbcUserRepository(Database database) {
        this.database = database;
    }

    @Override
    public User save(User user) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }

    @Override
    public Optional<User> findById(long id) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }

    @Override
    public Optional<User> findByEmail(String email) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }
}
