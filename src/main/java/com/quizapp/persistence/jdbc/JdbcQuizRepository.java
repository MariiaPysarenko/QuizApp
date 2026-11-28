package com.quizapp.persistence.jdbc;

import com.quizapp.common.NotFoundException;
import com.quizapp.domain.AnswerOption;
import com.quizapp.domain.Question;
import com.quizapp.domain.Quiz;
import com.quizapp.domain.QuizStatus;
import com.quizapp.domain.ScoringMode;
import com.quizapp.quiz.QuizRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.quizapp.persistence.jdbc.JdbcSupport.getLong;
import static com.quizapp.persistence.jdbc.JdbcSupport.insertReturningId;
import static com.quizapp.persistence.jdbc.JdbcSupport.setLong;

/**
 * Квізи в PostgreSQL: таблиці {@code quiz}, {@code question}, {@code answer_option}.
 * Новий квіз з усіма запитаннями й варіантами зберігається в одній транзакції.
 */
public class JdbcQuizRepository implements QuizRepository {

    private static final String QUIZ_COLUMNS =
            "id, creator_id, title, description, scoring_mode, time_limit_seconds, status";

    private final Database database;

    public JdbcQuizRepository(Database database) {
        this.database = database;
    }

    /**
     * Новий квіз (id = null) вставляється разом із запитаннями та варіантами.
     * Для наявного квізу оновлюються його поля (назва, статус тощо); запитання не змінюються.
     */
    @Override
    public Quiz save(Quiz quiz) {
        return database.inTransaction(connection ->
                quiz.id() == null ? insert(connection, quiz) : update(connection, quiz));
    }

    @Override
    public Optional<Quiz> findById(long id) {
        return database.withConnection(connection -> findQuiz(connection, id));
    }

    @Override
    public List<Quiz> findAll() {
        return database.withConnection(connection -> {
            List<Long> ids = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM quiz ORDER BY id");
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong("id"));
                }
            }
            List<Quiz> quizzes = new ArrayList<>();
            for (long id : ids) {
                findQuiz(connection, id).ifPresent(quizzes::add);
            }
            return quizzes;
        });
    }

    @Override
    public Optional<Question> findQuestionById(long questionId) {
        return database.withConnection(connection -> {
            String sql = "SELECT id, text, points, explanation FROM question WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, questionId);
                try (ResultSet rs = statement.executeQuery()) {
                    return rs.next() ? Optional.of(mapQuestion(connection, rs)) : Optional.empty();
                }
            }
        });
    }

    // ---------- запис ----------

    private static Quiz insert(Connection connection, Quiz quiz) throws SQLException {
        String sql = "INSERT INTO quiz (creator_id, title, description, scoring_mode, time_limit_seconds, status) "
                + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
        long quizId;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindQuizFields(statement, quiz);
            quizId = insertReturningId(statement);
        }
        List<Question> questions = new ArrayList<>();
        for (int i = 0; i < quiz.questions().size(); i++) {
            questions.add(insertQuestion(connection, quizId, i, quiz.questions().get(i)));
        }
        return new Quiz(quizId, quiz.creatorId(), quiz.title(), quiz.description(), quiz.scoringMode(),
                quiz.timeLimit(), quiz.status(), questions);
    }

    private static Quiz update(Connection connection, Quiz quiz) throws SQLException {
        String sql = "UPDATE quiz SET creator_id = ?, title = ?, description = ?, scoring_mode = ?, "
                + "time_limit_seconds = ?, status = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindQuizFields(statement, quiz);
            statement.setLong(7, quiz.id());
            if (statement.executeUpdate() == 0) {
                throw new NotFoundException("Квіз не знайдено");
            }
        }
        return quiz;
    }

    private static void bindQuizFields(PreparedStatement statement, Quiz quiz) throws SQLException {
        setLong(statement, 1, quiz.creatorId());
        statement.setString(2, quiz.title());
        statement.setString(3, quiz.description());
        statement.setString(4, quiz.scoringMode().name());
        setLong(statement, 5, quiz.timeLimit() == null ? null : quiz.timeLimit().toSeconds());
        statement.setString(6, quiz.status().name());
    }

    private static Question insertQuestion(Connection connection, long quizId, int position, Question question)
            throws SQLException {
        String sql = "INSERT INTO question (quiz_id, position, text, points, explanation) "
                + "VALUES (?, ?, ?, ?, ?) RETURNING id";
        long questionId;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, quizId);
            statement.setInt(2, position);
            statement.setString(3, question.text());
            statement.setInt(4, question.points());
            statement.setString(5, question.explanation());
            questionId = insertReturningId(statement);
        }
        String optionSql = "INSERT INTO answer_option (question_id, position, text, is_correct) "
                + "VALUES (?, ?, ?, ?) RETURNING id";
        List<AnswerOption> options = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(optionSql)) {
            for (int i = 0; i < question.options().size(); i++) {
                AnswerOption option = question.options().get(i);
                statement.setLong(1, questionId);
                statement.setInt(2, i);
                statement.setString(3, option.text());
                statement.setBoolean(4, option.correct());
                options.add(new AnswerOption(insertReturningId(statement), option.text(), option.correct()));
            }
        }
        return new Question(questionId, question.text(), question.points(), question.explanation(), options);
    }

    // ---------- читання ----------

    private static Optional<Quiz> findQuiz(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT " + QUIZ_COLUMNS + " FROM quiz WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Long seconds = getLong(rs, "time_limit_seconds");
                return Optional.of(new Quiz(
                        rs.getLong("id"),
                        getLong(rs, "creator_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        ScoringMode.valueOf(rs.getString("scoring_mode")),
                        seconds == null ? null : Duration.ofSeconds(seconds),
                        QuizStatus.valueOf(rs.getString("status")),
                        loadQuestions(connection, id)));
            }
        }
    }

    private static List<Question> loadQuestions(Connection connection, long quizId) throws SQLException {
        String sql = "SELECT id, text, points, explanation FROM question WHERE quiz_id = ? ORDER BY position";
        List<Question> questions = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, quizId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapQuestion(connection, rs));
                }
            }
        }
        return questions;
    }

    private static Question mapQuestion(Connection connection, ResultSet rs) throws SQLException {
        long questionId = rs.getLong("id");
        return new Question(questionId, rs.getString("text"), rs.getInt("points"), rs.getString("explanation"),
                loadOptions(connection, questionId));
    }

    private static List<AnswerOption> loadOptions(Connection connection, long questionId) throws SQLException {
        String sql = "SELECT id, text, is_correct FROM answer_option WHERE question_id = ? ORDER BY position";
        List<AnswerOption> options = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, questionId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    options.add(new AnswerOption(rs.getLong("id"), rs.getString("text"), rs.getBoolean("is_correct")));
                }
            }
        }
        return options;
    }
}
