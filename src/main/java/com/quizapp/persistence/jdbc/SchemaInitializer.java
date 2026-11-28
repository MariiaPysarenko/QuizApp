package com.quizapp.persistence.jdbc;

import java.util.List;

/**
 * Створює таблиці з {@code db/schema.sql}, якщо їх ще немає.
 * Поки що заглушка — тести мають падати.
 */
public final class SchemaInitializer {

    private final Database database;

    public SchemaInitializer(Database database) {
        this.database = database;
    }

    public void run() {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }

    static List<String> statements(String script) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }
}
