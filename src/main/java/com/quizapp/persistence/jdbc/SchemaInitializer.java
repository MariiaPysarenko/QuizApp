package com.quizapp.persistence.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Створює таблиці з {@code db/schema.sql}, якщо їх ще немає.
 * Завдяки {@code IF NOT EXISTS} його безпечно запускати при кожному старті застосунку.
 */
public final class SchemaInitializer {

    static final String SCHEMA_RESOURCE = "/db/schema.sql";

    private final Database database;

    public SchemaInitializer(Database database) {
        this.database = database;
    }

    public void run() {
        List<String> statements = statements(readSchema());
        database.withConnection(connection -> {
            try (Statement statement = connection.createStatement()) {
                for (String sql : statements) {
                    statement.execute(sql);
                }
            }
            return null;
        });
    }

    /** Окремі SQL-команди: без рядків-коментарів, розділені крапкою з комою. */
    static List<String> statements(String script) {
        String withoutComments = script.lines()
                .filter(line -> !line.strip().startsWith("--"))
                .collect(Collectors.joining("\n"));
        return Arrays.stream(withoutComments.split(";"))
                .map(String::strip)
                .filter(sql -> !sql.isEmpty())
                .toList();
    }

    private static String readSchema() {
        try (InputStream in = SchemaInitializer.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Не знайдено " + SCHEMA_RESOURCE);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
