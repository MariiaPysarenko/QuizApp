package com.quizapp.attempt;

import com.quizapp.attempt.scoring.QuestionResult;
import com.quizapp.attempt.scoring.ScoringResult;
import com.quizapp.common.AccessDeniedException;
import com.quizapp.common.ConflictException;
import com.quizapp.common.NotFoundException;
import com.quizapp.common.ValidationException;
import com.quizapp.domain.AnswerOption;
import com.quizapp.domain.Attempt;
import com.quizapp.domain.AttemptStatus;
import com.quizapp.domain.Question;
import com.quizapp.domain.Quiz;
import com.quizapp.domain.QuizStatus;
import com.quizapp.domain.ScoringMode;
import com.quizapp.domain.SubmittedAnswer;
import com.quizapp.persistence.memory.InMemoryAttemptRepository;
import com.quizapp.persistence.memory.InMemoryQuizRepository;
import com.quizapp.quiz.QuizBuilder;
import com.quizapp.testsupport.TestClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttemptServiceTest {

    private static final long STUDENT = 1;
    private static final long OTHER_STUDENT = 2;
    private static final Instant START = Instant.parse("2026-11-28T10:00:00Z");

    private final InMemoryQuizRepository quizzes = new InMemoryQuizRepository();
    private final InMemoryAttemptRepository attempts = new InMemoryAttemptRepository();
    private final TestClock clock = new TestClock(START);
    private final AttemptService service = new AttemptService(quizzes, attempts, clock);

    // ---------- start ----------

    @Test
    @DisplayName("Студент починає спробу опублікованого квізу")
    void startsAttemptForPublishedQuiz() {
        Quiz quiz = savePublishedQuiz(new QuizBuilder());

        Attempt attempt = service.start(STUDENT, quiz.id());

        assertThat(attempt.id()).isNotNull();
        assertThat(attempt.userId()).isEqualTo(STUDENT);
        assertThat(attempt.quizId()).isEqualTo(quiz.id());
        assertThat(attempt.status()).isEqualTo(AttemptStatus.IN_PROGRESS);
        assertThat(attempt.startedAt()).isEqualTo(START);
    }

    @Test
    @DisplayName("Чернетку квізу пройти не можна")
    void cannotStartDraftQuiz() {
        Quiz draft = quizzes.save(twoQuestionQuiz(new QuizBuilder()).build());

        assertThatThrownBy(() -> service.start(STUDENT, draft.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Не можна почати спробу неіснуючого квізу")
    void cannotStartMissingQuiz() {
        assertThatThrownBy(() -> service.start(STUDENT, 999))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- submit ----------

    @Test
    @DisplayName("Відповіді оцінюються, а спроба завершується та зберігається")
    void submitScoresAndCompletesAttempt() {
        Quiz quiz = savePublishedQuiz(new QuizBuilder());
        Attempt attempt = service.start(STUDENT, quiz.id());
        clock.advance(Duration.ofMinutes(3));

        ScoringResult result = service.submit(STUDENT, attempt.id(), Map.of(
                questionId(quiz, 0), optionId(quiz, 0, true),
                questionId(quiz, 1), optionId(quiz, 1, false)));

        assertThat(result.score()).isEqualTo(2);
        assertThat(result.maxScore()).isEqualTo(3);

        Attempt stored = attempts.findById(attempt.id()).orElseThrow();
        assertThat(stored.status()).isEqualTo(AttemptStatus.COMPLETED);
        assertThat(stored.score()).isEqualTo(2);
        assertThat(stored.maxScore()).isEqualTo(3);
        assertThat(stored.completedAt()).isEqualTo(START.plus(Duration.ofMinutes(3)));
        assertThat(stored.answers())
                .extracting(SubmittedAnswer::correct)
                .containsExactly(true, false);
    }

    @Test
    @DisplayName("Запитання без відповіді вважається пропущеним")
    void missingAnswerCountsAsSkipped() {
        Quiz quiz = savePublishedQuiz(new QuizBuilder());
        Attempt attempt = service.start(STUDENT, quiz.id());

        ScoringResult result = service.submit(STUDENT, attempt.id(), Map.of(
                questionId(quiz, 0), optionId(quiz, 0, true)));

        assertThat(result.mistakes())
                .extracting(QuestionResult::question)
                .extracting(Question::id)
                .containsExactly(questionId(quiz, 1));
        Attempt stored = attempts.findById(attempt.id()).orElseThrow();
        assertThat(stored.answers().get(1).selectedOptionId()).isNull();
    }

    @Test
    @DisplayName("Використовується спосіб оцінювання, заданий у квізі")
    void usesScoringModeOfQuiz() {
        Quiz quiz = savePublishedQuiz(new QuizBuilder().scoringMode(ScoringMode.PENALTY));
        Attempt attempt = service.start(STUDENT, quiz.id());

        ScoringResult result = service.submit(STUDENT, attempt.id(), Map.of(
                questionId(quiz, 0), optionId(quiz, 0, true),
                questionId(quiz, 1), optionId(quiz, 1, false)));

        assertThat(result.score()).isEqualTo(1.75);
    }

    @Test
    @DisplayName("Не можна надіслати відповіді до чужої спроби")
    void cannotSubmitSomeoneElsesAttempt() {
        Quiz quiz = savePublishedQuiz(new QuizBuilder());
        Attempt attempt = service.start(STUDENT, quiz.id());

        assertThatThrownBy(() -> service.submit(OTHER_STUDENT, attempt.id(), Map.of()))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(attempts.findById(attempt.id()).orElseThrow().status())
                .isEqualTo(AttemptStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Повторне надсилання не змінює результат")
    void cannotSubmitTwice() {
        Quiz quiz = savePublishedQuiz(new QuizBuilder());
        Attempt attempt = service.start(STUDENT, quiz.id());
        service.submit(STUDENT, attempt.id(), Map.of(
                questionId(quiz, 0), optionId(quiz, 0, false)));

        assertThatThrownBy(() -> service.submit(STUDENT, attempt.id(), Map.of(
                questionId(quiz, 0), optionId(quiz, 0, true),
                questionId(quiz, 1), optionId(quiz, 1, true))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("завершена");
        assertThat(attempts.findById(attempt.id()).orElseThrow().score()).isEqualTo(0);
    }

    @Test
    @DisplayName("Не можна надіслати відповіді до неіснуючої спроби")
    void cannotSubmitMissingAttempt() {
        assertThatThrownBy(() -> service.submit(STUDENT, 999, Map.of()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Відповідь на запитання з іншого квізу відхиляється")
    void rejectsQuestionFromAnotherQuiz() {
        Quiz quiz = savePublishedQuiz(new QuizBuilder());
        Quiz otherQuiz = savePublishedQuiz(new QuizBuilder());
        Attempt attempt = service.start(STUDENT, quiz.id());

        assertThatThrownBy(() -> service.submit(STUDENT, attempt.id(), Map.of(
                questionId(otherQuiz, 0), optionId(otherQuiz, 0, true))))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("Варіант з іншого запитання відхиляється")
    void rejectsOptionFromAnotherQuestion() {
        Quiz quiz = savePublishedQuiz(new QuizBuilder());
        Attempt attempt = service.start(STUDENT, quiz.id());

        assertThatThrownBy(() -> service.submit(STUDENT, attempt.id(), Map.of(
                questionId(quiz, 0), optionId(quiz, 1, true))))
                .isInstanceOf(ValidationException.class);
    }

    // ---------- history ----------

    @Test
    @DisplayName("Історія містить лише завершені спроби студента в порядку завершення")
    void historyContainsCompletedAttemptsInOrder() {
        Quiz quiz = savePublishedQuiz(new QuizBuilder());
        Attempt first = service.start(STUDENT, quiz.id());
        service.submit(STUDENT, first.id(), Map.of());
        clock.advance(Duration.ofMinutes(1));
        Attempt second = service.start(STUDENT, quiz.id());
        service.submit(STUDENT, second.id(), Map.of());
        service.start(STUDENT, quiz.id());
        Attempt other = service.start(OTHER_STUDENT, quiz.id());
        service.submit(OTHER_STUDENT, other.id(), Map.of());

        assertThat(service.history(STUDENT))
                .extracting(Attempt::id)
                .containsExactly(first.id(), second.id());
    }

    // ---------- time limit ----------

    @Test
    @DisplayName("Відповіді в межах часу (з 30-секундним запасом) приймаються")
    void acceptsSubmitWithinTimeLimit() {
        Quiz quiz = savePublishedQuiz(new QuizBuilder().timeLimit(Duration.ofMinutes(1)));
        Attempt attempt = service.start(STUDENT, quiz.id());
        clock.advance(Duration.ofSeconds(80));

        service.submit(STUDENT, attempt.id(), Map.of());

        assertThat(attempts.findById(attempt.id()).orElseThrow().status())
                .isEqualTo(AttemptStatus.COMPLETED);
    }

    @Test
    @DisplayName("Після завершення часу спроба позначається як прострочена")
    void expiresAttemptAfterTimeLimit() {
        Quiz quiz = savePublishedQuiz(new QuizBuilder().timeLimit(Duration.ofMinutes(1)));
        Attempt attempt = service.start(STUDENT, quiz.id());
        clock.advance(Duration.ofSeconds(91));

        assertThatThrownBy(() -> service.submit(STUDENT, attempt.id(), Map.of(
                questionId(quiz, 0), optionId(quiz, 0, true))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Час");
        Attempt stored = attempts.findById(attempt.id()).orElseThrow();
        assertThat(stored.status()).isEqualTo(AttemptStatus.EXPIRED);
        assertThat(stored.score()).isEqualTo(0);
    }

    // ---------- helpers ----------

    /** Квіз із двох запитань: на 2 бали і на 1 бал. */
    private static QuizBuilder twoQuestionQuiz(QuizBuilder builder) {
        return builder
                .title("Основи Java")
                .question("Який модифікатор робить поле видимим лише в класі?")
                .wrong("public")
                .correct("private")
                .points(2)
                .question("Що виконує байт-код?")
                .correct("JVM")
                .wrong("javac");
    }

    private Quiz savePublishedQuiz(QuizBuilder builder) {
        return quizzes.save(twoQuestionQuiz(builder).status(QuizStatus.PUBLISHED).build());
    }

    private static Long questionId(Quiz quiz, int index) {
        return quiz.questions().get(index).id();
    }

    private static Long optionId(Quiz quiz, int questionIndex, boolean correct) {
        return quiz.questions().get(questionIndex).options().stream()
                .filter(option -> option.correct() == correct)
                .map(AnswerOption::id)
                .findFirst()
                .orElseThrow();
    }
}
