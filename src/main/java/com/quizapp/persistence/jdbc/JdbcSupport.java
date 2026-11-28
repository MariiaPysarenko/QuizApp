package com.quizapp.persistence.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Перетворення типів Java ↔ SQL, спільні для всіх JDBC-репозиторіїв.
 */
final class JdbcSupport {

    /** SQLSTATE PostgreSQL для порушення UNIQUE. */
    static final String UNIQUE_VIOLATION = "23505";

    private JdbcSupport() {
    }

    static void setInstant(PreparedStatement statement, int index, Instant value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.TIMESTAMP_WITH_TIMEZONE);
        } else {
            statement.setObject(index, OffsetDateTime.ofInstant(value, ZoneOffset.UTC));
        }
    }

    static Instant getInstant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    static void setLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    static Long getLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    /** Виконує INSERT … RETURNING id і повертає згенерований id. */
    static long insertReturningId(PreparedStatement statement) throws SQLException {
        try (ResultSet rs = statement.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
