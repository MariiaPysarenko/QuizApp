package com.quizapp.persistence;

import com.quizapp.persistence.jdbc.Database;
import com.quizapp.persistence.jdbc.JdbcAttemptRepository;
import com.quizapp.persistence.jdbc.JdbcQuizRepository;
import com.quizapp.persistence.jdbc.JdbcUserRepository;
import com.quizapp.persistence.jdbc.SchemaInitializer;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Statement;

/**
 * Той самий контракт для PostgreSQL. Запускається, лише якщо задано змінні середовища
 * TEST_DB_URL, TEST_DB_USER, TEST_DB_PASSWORD (окрема тестова база — її таблиці очищаються).
 * Без них тести пропускаються, і {@code mvn test} працює на будь-якому комп'ютері.
 */
@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".+")
class JdbcRepositoryContractTest extends RepositoryContractTest {

    @Override
    protected void createRepositories() {
        Database database = new Database(
                System.getenv("TEST_DB_URL"), System.getenv("TEST_DB_USER"), System.getenv("TEST_DB_PASSWORD"));
        new SchemaInitializer(database).run();
        database.withConnection(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("TRUNCATE submitted_answer, attempt, answer_option, question, quiz, users "
                        + "RESTART IDENTITY CASCADE");
            }
            return null;
        });
        users = new JdbcUserRepository(database);
        quizzes = new JdbcQuizRepository(database);
        attempts = new JdbcAttemptRepository(database);
    }
}
