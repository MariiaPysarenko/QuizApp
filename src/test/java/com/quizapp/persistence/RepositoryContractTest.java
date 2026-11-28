package com.quizapp.persistence;

import com.quizapp.attempt.AttemptRepository;
import com.quizapp.auth.UserRepository;
import com.quizapp.common.ConflictException;
import com.quizapp.domain.Attempt;
import com.quizapp.domain.AttemptStatus;
import com.quizapp.domain.AttemptType;
import com.quizapp.domain.Question;
import com.quizapp.domain.Quiz;
import com.quizapp.domain.QuizStatus;
import com.quizapp.domain.Role;
import com.quizapp.domain.ScoringMode;
import com.quizapp.domain.SubmittedAnswer;
import com.quizapp.domain.User;
import com.quizapp.quiz.QuizBuilder;
import com.quizapp.quiz.QuizRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Контракт репозиторіїв: однакові тести для реалізації в пам'яті та для PostgreSQL.
 * Якщо обидві проходять, сервіси можна безпечно перемикати між ними (патерн Repository).
 */
abstract class RepositoryContractTest {

    private static final Instant T0 = Instant.parse("2026-11-28T10:00:00Z");

    protected UserRepository users;
    protected QuizRepository quizzes;
    protected AttemptRepository attempts;

    /** Порожні репозиторії перед кожним тестом. */
    protected abstract void createRepositories();

    @BeforeEach
    void setUpRepositories() {
        createRepositories();
    }

    // ---------- users ----------

    @Test
    @DisplayName("Користувач зберігається, отримує id і знаходиться за id та email")
    void savesAndFindsUser() {
        User saved = users.save(new User(null, "maria@example.com", "hash", Role.TEACHER, T0));

        assertThat(saved.id()).isNotNull();
        assertThat(users.findById(saved.id()).orElseThrow()).isEqualTo(saved);
        assertThat(users.findByEmail("maria@example.com").orElseThrow()).isEqualTo(saved);
        assertThat(users.findByEmail("nobody@example.com").isPresent()).isFalse();
        assertThat(users.findById(999_999).isPresent()).isFalse();
    }

    @Test
    @DisplayName("Email унікальний")
    void emailIsUnique() {
        users.save(new User(null, "maria@example.com", "hash", Role.STUDENT, T0));

        assertThatThrownBy(() -> users.save(new User(null, "maria@example.com", "other", Role.STUDENT, T0)))
                .isInstanceOf(ConflictException.class);
    }

    // ---------- quizzes ----------

    @Test
    @DisplayName("Квіз зберігається з усіма запитаннями й варіантами та читається без змін")
    void savesQuizWithQuestionsAndOptions() {
        User teacher = teacher();

        Quiz saved = quizzes.save(fullQuiz(teacher.id()));

        assertThat(saved.id()).isNotNull();
        assertThat(saved.questions().get(0).id()).isNotNull();
        assertThat(saved.questions().get(0).options().get(2).id()).isNotNull();
        assertThat(quizzes.findById(saved.id()).orElseThrow()).isEqualTo(saved);
    }

    @Test
    @DisplayName("Необов'язкові поля можуть бути порожніми")
    void savesQuizWithoutOptionalFields() {
        Quiz saved = quizzes.save(new QuizBuilder()
                .title("Мінімальний")
                .question("2 + 2 = ?").correct("4").wrong("5")
                .build());

        Quiz found = quizzes.findById(saved.id()).orElseThrow();
        assertThat(found).isEqualTo(saved);
        assertThat(found.creatorId()).isNull();
        assertThat(found.description()).isNull();
        assertThat(found.timeLimit()).isNull();
        assertThat(found.questions().get(0).explanation()).isNull();
    }

    @Test
    @DisplayName("Збереження наявного квізу оновлює статус і не чіпає запитання")
    void updatesExistingQuiz() {
        Quiz saved = quizzes.save(fullQuiz(teacher().id()));

        quizzes.save(saved.withStatus(QuizStatus.PUBLISHED));

        Quiz found = quizzes.findById(saved.id()).orElseThrow();
        assertThat(found.status()).isEqualTo(QuizStatus.PUBLISHED);
        assertThat(found.questions()).isEqualTo(saved.questions());
    }

    @Test
    @DisplayName("Усі квізи повертаються в порядку створення")
    void findsAllQuizzesInOrder() {
        Quiz first = quizzes.save(fullQuiz(teacher().id()));
        Quiz second = quizzes.save(new QuizBuilder().title("Другий").question("A").correct("+").wrong("-").build());

        assertThat(quizzes.findAll())
                .extracting(Quiz::id)
                .containsExactly(first.id(), second.id());
    }

    @Test
    @DisplayName("Запитання знаходиться за id разом із варіантами")
    void findsQuestionById() {
        Quiz saved = quizzes.save(fullQuiz(teacher().id()));
        Question second = saved.questions().get(1);

        assertThat(quizzes.findQuestionById(second.id()).orElseThrow()).isEqualTo(second);
        assertThat(quizzes.findQuestionById(999_999).isPresent()).isFalse();
        assertThat(quizzes.findById(999_999).isPresent()).isFalse();
    }

    // ---------- attempts ----------

    @Test
    @DisplayName("Нова спроба зберігається й читається")
    void savesNewAttempt() {
        User student = student();
        Quiz quiz = quizzes.save(fullQuiz(teacher().id()));

        Attempt saved = attempts.save(Attempt.startQuiz(student.id(), quiz.id(), T0));

        assertThat(saved.id()).isNotNull();
        assertThat(attempts.findById(saved.id()).orElseThrow()).isEqualTo(saved);
        assertThat(attempts.findById(999_999).isPresent()).isFalse();
    }

    @Test
    @DisplayName("Завершена спроба зберігає бали та відповіді в порядку, включно з пропущеними")
    void completesAttemptWithAnswers() {
        User student = student();
        Quiz quiz = quizzes.save(fullQuiz(teacher().id()));
        Attempt started = attempts.save(Attempt.startQuiz(student.id(), quiz.id(), T0));
        Question q1 = quiz.questions().get(0);
        Question q2 = quiz.questions().get(1);
        List<SubmittedAnswer> answers = List.of(
                new SubmittedAnswer(q1.id(), q1.options().get(1).id(), true),
                new SubmittedAnswer(q2.id(), null, false));

        attempts.save(started.complete(2, 3, answers, T0.plusSeconds(90)));

        Attempt found = attempts.findById(started.id()).orElseThrow();
        assertThat(found.status()).isEqualTo(AttemptStatus.COMPLETED);
        assertThat(found.score()).isEqualTo(2);
        assertThat(found.maxScore()).isEqualTo(3);
        assertThat(found.completedAt()).isEqualTo(T0.plusSeconds(90));
        assertThat(found.answers()).isEqualTo(answers);
    }

    @Test
    @DisplayName("Завершену спробу не можна перезаписати")
    void cannotOverwriteFinishedAttempt() {
        User student = student();
        Quiz quiz = quizzes.save(fullQuiz(teacher().id()));
        Attempt started = attempts.save(Attempt.startQuiz(student.id(), quiz.id(), T0));
        attempts.save(started.complete(1, 3, List.of(), T0.plusSeconds(60)));

        assertThatThrownBy(() -> attempts.save(started.complete(3, 3, List.of(), T0.plusSeconds(70))))
                .isInstanceOf(ConflictException.class);
        assertThat(attempts.findById(started.id()).orElseThrow().score()).isEqualTo(1);
    }

    @Test
    @DisplayName("Історія — лише завершені спроби користувача, за часом завершення, з практикою")
    void findsCompletedAttemptsOfUser() {
        User student = student();
        User other = users.save(new User(null, "other@example.com", "hash", Role.STUDENT, T0));
        Quiz quiz = quizzes.save(fullQuiz(teacher().id()));

        Attempt late = attempts.save(Attempt.startQuiz(student.id(), quiz.id(), T0));
        attempts.save(late.complete(1, 3, List.of(), T0.plusSeconds(300)));
        Attempt practice = attempts.save(Attempt.completedPractice(student.id(), 1, 1, List.of(), T0.plusSeconds(100)));
        attempts.save(Attempt.startQuiz(student.id(), quiz.id(), T0));
        Attempt foreign = attempts.save(Attempt.startQuiz(other.id(), quiz.id(), T0));
        attempts.save(foreign.complete(0, 3, List.of(), T0.plusSeconds(50)));

        List<Attempt> history = attempts.findCompletedByUser(student.id());

        assertThat(history).extracting(Attempt::id).containsExactly(practice.id(), late.id());
        assertThat(history.get(0).type()).isEqualTo(AttemptType.PRACTICE);
        assertThat(history.get(0).quizId()).isNull();
    }

    // ---------- helpers ----------

    private User teacher() {
        return users.save(new User(null, "teacher@example.com", "hash", Role.TEACHER, T0));
    }

    private User student() {
        return users.save(new User(null, "student@example.com", "hash", Role.STUDENT, T0));
    }

    private static Quiz fullQuiz(long creatorId) {
        return new QuizBuilder()
                .creatorId(creatorId)
                .title("Основи Java")
                .description("Квіз з усіма полями")
                .scoringMode(ScoringMode.PENALTY)
                .timeLimit(Duration.ofMinutes(10))
                .status(QuizStatus.DRAFT)
                .question("Який модифікатор приховує поле?")
                .wrong("public").correct("private").wrong("protected")
                .points(2)
                .explanation("Інкапсуляція")
                .question("Що виконує байт-код?")
                .correct("JVM").wrong("javac")
                .build();
    }
}
