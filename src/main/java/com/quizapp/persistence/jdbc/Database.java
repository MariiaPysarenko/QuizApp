package com.quizapp.persistence.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Підключення до PostgreSQL і виконання роботи в транзакції.
 * <p>
 * Кожен виклик відкриває нове з'єднання й закриває його після роботи. Для навчального
 * навантаження цього достатньо; пул з'єднань можна додати пізніше, не змінюючи репозиторіїв.
 */
public final class Database {

    private final String url;
    private final String user;
    private final String password;

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /** Робота, яка може кинути {@link SQLException}. */
    @FunctionalInterface
    public interface SqlWork<T> {
        T run(Connection connection) throws SQLException;
    }

    /** Виконує роботу з окремим з'єднанням без явної транзакції. */
    public <T> T withConnection(SqlWork<T> work) {
        try (Connection connection = connect()) {
            return work.run(connection);
        } catch (SQLException e) {
            throw new DataAccessException("Помилка роботи з базою даних", e);
        }
    }

    /**
     * Виконує роботу в одній транзакції: або всі зміни зберігаються, або жодна.
     * Будь-який виняток (SQL чи бізнес-правило) скасовує транзакцію.
     */
    public <T> T inTransaction(SqlWork<T> work) {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Помилка роботи з базою даних", e);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
