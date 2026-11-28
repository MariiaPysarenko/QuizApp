package com.quizapp.persistence.jdbc;

import com.quizapp.auth.UserRepository;
import com.quizapp.common.ConflictException;
import com.quizapp.domain.Role;
import com.quizapp.domain.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static com.quizapp.persistence.jdbc.JdbcSupport.getInstant;
import static com.quizapp.persistence.jdbc.JdbcSupport.insertReturningId;
import static com.quizapp.persistence.jdbc.JdbcSupport.setInstant;

/**
 * Користувачі в PostgreSQL (таблиця {@code users}).
 */
public class JdbcUserRepository implements UserRepository {

    private static final String COLUMNS = "id, email, password_hash, role, created_at";

    private final Database database;

    public JdbcUserRepository(Database database) {
        this.database = database;
    }

    @Override
    public User save(User user) {
        return database.inTransaction(connection -> {
            try {
                return user.id() == null ? insert(connection, user) : update(connection, user);
            } catch (SQLException e) {
                if (JdbcSupport.UNIQUE_VIOLATION.equals(e.getSQLState())) {
                    throw new ConflictException("Користувач з таким email уже існує");
                }
                throw e;
            }
        });
    }

    @Override
    public Optional<User> findById(long id) {
        return findOne("SELECT " + COLUMNS + " FROM users WHERE id = ?", statement -> statement.setLong(1, id));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return findOne("SELECT " + COLUMNS + " FROM users WHERE email = ?", statement -> statement.setString(1, email));
    }

    private static User insert(Connection connection, User user) throws SQLException {
        String sql = "INSERT INTO users (email, password_hash, role, created_at) VALUES (?, ?, ?, ?) RETURNING id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.email());
            statement.setString(2, user.passwordHash());
            statement.setString(3, user.role().name());
            setInstant(statement, 4, user.createdAt());
            long id = insertReturningId(statement);
            return new User(id, user.email(), user.passwordHash(), user.role(), user.createdAt());
        }
    }

    private static User update(Connection connection, User user) throws SQLException {
        String sql = "UPDATE users SET email = ?, password_hash = ?, role = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.email());
            statement.setString(2, user.passwordHash());
            statement.setString(3, user.role().name());
            statement.setLong(4, user.id());
            statement.executeUpdate();
            return user;
        }
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    private Optional<User> findOne(String sql, Binder binder) {
        return database.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                binder.bind(statement);
                try (ResultSet rs = statement.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.empty();
                }
            }
        });
    }

    private static User map(ResultSet rs) throws SQLException {
        return new User(rs.getLong("id"), rs.getString("email"), rs.getString("password_hash"),
                Role.valueOf(rs.getString("role")), getInstant(rs, "created_at"));
    }
}
