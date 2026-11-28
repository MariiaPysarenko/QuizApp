package com.quizapp.persistence.jdbc;

import com.quizapp.attempt.AttemptRepository;
import com.quizapp.common.ConflictException;
import com.quizapp.domain.Attempt;
import com.quizapp.domain.AttemptStatus;
import com.quizapp.domain.AttemptType;
import com.quizapp.domain.SubmittedAnswer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.quizapp.persistence.jdbc.JdbcSupport.getInstant;
import static com.quizapp.persistence.jdbc.JdbcSupport.getLong;
import static com.quizapp.persistence.jdbc.JdbcSupport.insertReturningId;
import static com.quizapp.persistence.jdbc.JdbcSupport.setInstant;
import static com.quizapp.persistence.jdbc.JdbcSupport.setLong;

/**
 * Спроби в PostgreSQL: таблиці {@code attempt} і {@code submitted_answer}.
 * <p>
 * Оновити можна лише спробу зі статусом IN_PROGRESS: умова стоїть прямо в UPDATE,
 * тому навіть два одночасні запити не зарахують результат двічі (NFR-5).
 */
public class JdbcAttemptRepository implements AttemptRepository {

    private static final String COLUMNS =
            "id, user_id, quiz_id, type, status, started_at, completed_at, score, max_score";

    private final Database database;

    public JdbcAttemptRepository(Database database) {
        this.database = database;
    }

    @Override
    public Attempt save(Attempt attempt) {
        return database.inTransaction(connection ->
                attempt.id() == null ? insert(connection, attempt) : update(connection, attempt));
    }

    @Override
    public Optional<Attempt> findById(long id) {
        return database.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM attempt WHERE id = ?")) {
                statement.setLong(1, id);
                try (ResultSet rs = statement.executeQuery()) {
                    return rs.next() ? Optional.of(map(connection, rs)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<Attempt> findCompletedByUser(long userId) {
        return database.withConnection(connection -> {
            String sql = "SELECT " + COLUMNS + " FROM attempt WHERE user_id = ? AND status = ? "
                    + "ORDER BY completed_at, id";
            List<Attempt> result = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, userId);
                statement.setString(2, AttemptStatus.COMPLETED.name());
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        result.add(map(connection, rs));
                    }
                }
            }
            return result;
        });
    }

    // ---------- запис ----------

    private static Attempt insert(Connection connection, Attempt attempt) throws SQLException {
        String sql = "INSERT INTO attempt (user_id, quiz_id, type, status, started_at, completed_at, score, max_score) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        long id;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, attempt.userId());
            setLong(statement, 2, attempt.quizId());
            statement.setString(3, attempt.type().name());
            statement.setString(4, attempt.status().name());
            setInstant(statement, 5, attempt.startedAt());
            setInstant(statement, 6, attempt.completedAt());
            statement.setDouble(7, attempt.score());
            statement.setDouble(8, attempt.maxScore());
            id = insertReturningId(statement);
        }
        insertAnswers(connection, id, attempt.answers());
        return new Attempt(id, attempt.userId(), attempt.quizId(), attempt.type(), attempt.status(),
                attempt.startedAt(), attempt.completedAt(), attempt.score(), attempt.maxScore(), attempt.answers());
    }

    private static Attempt update(Connection connection, Attempt attempt) throws SQLException {
        String sql = "UPDATE attempt SET status = ?, completed_at = ?, score = ?, max_score = ? "
                + "WHERE id = ? AND status = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, attempt.status().name());
            setInstant(statement, 2, attempt.completedAt());
            statement.setDouble(3, attempt.score());
            statement.setDouble(4, attempt.maxScore());
            statement.setLong(5, attempt.id());
            statement.setString(6, AttemptStatus.IN_PROGRESS.name());
            if (statement.executeUpdate() == 0) {
                throw new ConflictException("Спроба вже завершена");
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM submitted_answer WHERE attempt_id = ?")) {
            statement.setLong(1, attempt.id());
            statement.executeUpdate();
        }
        insertAnswers(connection, attempt.id(), attempt.answers());
        return attempt;
    }

    private static void insertAnswers(Connection connection, long attemptId, List<SubmittedAnswer> answers)
            throws SQLException {
        if (answers.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO submitted_answer (attempt_id, position, question_id, selected_option_id, is_correct) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < answers.size(); i++) {
                SubmittedAnswer answer = answers.get(i);
                statement.setLong(1, attemptId);
                statement.setInt(2, i);
                statement.setLong(3, answer.questionId());
                setLong(statement, 4, answer.selectedOptionId());
                statement.setBoolean(5, answer.correct());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    // ---------- читання ----------

    private static Attempt map(Connection connection, ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        return new Attempt(id,
                rs.getLong("user_id"),
                getLong(rs, "quiz_id"),
                AttemptType.valueOf(rs.getString("type")),
                AttemptStatus.valueOf(rs.getString("status")),
                getInstant(rs, "started_at"),
                getInstant(rs, "completed_at"),
                rs.getDouble("score"),
                rs.getDouble("max_score"),
                loadAnswers(connection, id));
    }

    private static List<SubmittedAnswer> loadAnswers(Connection connection, long attemptId) throws SQLException {
        String sql = "SELECT question_id, selected_option_id, is_correct FROM submitted_answer "
                + "WHERE attempt_id = ? ORDER BY position";
        List<SubmittedAnswer> answers = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, attemptId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    answers.add(new SubmittedAnswer(rs.getLong("question_id"),
                            getLong(rs, "selected_option_id"), rs.getBoolean("is_correct")));
                }
            }
        }
        return answers;
    }
}
