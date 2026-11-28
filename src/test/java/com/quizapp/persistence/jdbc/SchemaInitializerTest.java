package com.quizapp.persistence.jdbc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaInitializerTest {

    @Test
    @DisplayName("Скрипт ділиться на команди, коментарі й порожні рядки відкидаються")
    void splitsScriptIntoStatements() {
        String script = """
                -- коментар
                CREATE TABLE a (id INT);

                -- ще коментар
                CREATE INDEX i ON a (id);
                """;

        List<String> statements = SchemaInitializer.statements(script);

        assertThat(statements).containsExactly("CREATE TABLE a (id INT)", "CREATE INDEX i ON a (id)");
    }
}
