package com.quizapp.practice;

import com.quizapp.attempt.AttemptService;
import com.quizapp.attempt.scoring.ScoringResult;
import com.quizapp.common.ValidationException;
import com.quizapp.domain.AnswerOption;
import com.quizapp.domain.Attempt;
import com.quizapp.domain.AttemptStatus;
import com.quizapp.domain.AttemptType;
import com.quizapp.domain.Question;
import com.quizapp.domain.Quiz;
import com.quizapp.domain.QuizStatus;
import com.quizapp.persistence.memory.InMemoryAttemptRepository;
import com.quizapp.persistence.memory.InMemoryQuizRepository;
import com.quizapp.quiz.QuizBuilder;
import com.quizapp.testsupport.TestClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PracticeServiceTest {

    private static final long STUDENT = 1;
    private static final long OTHER_STUDENT = 2;

    private final InMemoryQuizRepository quizzes = new InMemoryQuizRepository();
    private final InMemoryAttemptRepository attempts = new InMemoryAttemptRepository();
    private final TestClock clock = new TestClock(Instant.parse("2026-11-28T10:00:00Z"));
    private final AttemptService attemptService = new AttemptService(quizzes, attempts, clock);
    private final PracticeService practice = new PracticeService(quizzes, attempts, clock);

    /** Квіз з трьох запитань: A, B, C — по 1 балу. */
    private final Quiz quiz = quizzes.save(new QuizBuilder()
            .title("Основи Java")
            .question("A").correct("A+").wrong("A-")
            .question("B").correct("B+").wrong("B-")
            .question("C").correct("C+").wrong("C-")
            .status(QuizStatus.PUBLISHED)
            .build());

    private final Question a = quiz.questions().get(0);
    private final Question b = quiz.questions().get(1);
    private final Question c = quiz.questions().get(2);

    @Test
    @DisplayName("У практику потрапляють неправильні та пропущені запитання")
    void practiceContainsWrongAndSkippedQuestions() {
        passQuiz(STUDENT, Map.of(a.id(), right(a), b.id(), wrong(b)));

        assertThat(practice.buildPractice(STUDENT))
                .extracting(Question::text)
                .containsExactly("B", "C");
    }

    @Test
    @DisplayName("Якщо помилок немає, добірка порожня")
    void emptyPracticeWithoutMistakes() {
        passQuiz(STUDENT, Map.of(a.id(), right(a), b.id(), right(b), c.id(), right(c)));

        assertThat(practice.buildPractice(STUDENT)).isEmpty();
    }

    @Test
    @DisplayName("У нового студента добірка порожня")
    void emptyPracticeForNewStudent() {
        assertThat(practice.buildPractice(STUDENT)).isEmpty();
    }

    @Test
    @DisplayName("Враховуються лише власні спроби студента")
    void usesOnlyOwnAttempts() {
        passQuiz(OTHER_STUDENT, Map.of());
        passQuiz(STUDENT, Map.of(a.id(), right(a), b.id(), right(b), c.id(), wrong(c)));

        assertThat(practice.buildPractice(STUDENT))
                .extracting(Question::text)
                .containsExactly("C");
    }

    @Test
    @DisplayName("Правильна відповідь у практиці прибирає запитання з добірки")
    void correctPracticeAnswerRemovesQuestion() {
        passQuiz(STUDENT, Map.of(a.id(), right(a)));
        clock.advance(Duration.ofMinutes(1));

        practice.submit(STUDENT, Map.of(b.id(), right(b), c.id(), wrong(c)));

        assertThat(practice.buildPractice(STUDENT))
                .extracting(Question::text)
                .containsExactly("C");
    }

    @Test
    @DisplayName("Нова помилка в квізі повертає запитання в добірку")
    void newMistakeReturnsQuestion() {
        passQuiz(STUDENT, Map.of(a.id(), right(a), b.id(), wrong(b), c.id(), right(c)));
        clock.advance(Duration.ofMinutes(1));
        practice.submit(STUDENT, Map.of(b.id(), right(b)));
        assertThat(practice.buildPractice(STUDENT)).isEmpty();
        clock.advance(Duration.ofMinutes(1));

        passQuiz(STUDENT, Map.of(a.id(), right(a), b.id(), wrong(b), c.id(), right(c)));

        assertThat(practice.buildPractice(STUDENT))
                .extracting(Question::text)
                .containsExactly("B");
    }

    @Test
    @DisplayName("Практика оцінюється та зберігається як завершена спроба типу PRACTICE")
    void practiceIsScoredAndSaved() {
        passQuiz(STUDENT, Map.of(a.id(), right(a)));
        clock.advance(Duration.ofMinutes(1));

        ScoringResult result = practice.submit(STUDENT, Map.of(b.id(), right(b), c.id(), wrong(c)));

        assertThat(result.score()).isEqualTo(1);
        assertThat(result.maxScore()).isEqualTo(2);
        List<Attempt> history = attempts.findCompletedByUser(STUDENT);
        Attempt saved = history.get(history.size() - 1);
        assertThat(saved.type()).isEqualTo(AttemptType.PRACTICE);
        assertThat(saved.quizId()).isNull();
        assertThat(saved.status()).isEqualTo(AttemptStatus.COMPLETED);
        assertThat(saved.answers()).hasSize(2);
    }

    @Test
    @DisplayName("Не можна надіслати відповідь на запитання, якого немає в добірці")
    void rejectsQuestionOutsidePractice() {
        passQuiz(STUDENT, Map.of(a.id(), right(a)));

        assertThatThrownBy(() -> practice.submit(STUDENT, Map.of(a.id(), right(a))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("добірці");
    }

    @Test
    @DisplayName("Порожню практику надіслати не можна")
    void rejectsEmptySubmit() {
        assertThatThrownBy(() -> practice.submit(STUDENT, Map.of()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("Варіант з іншого запитання відхиляється")
    void rejectsOptionFromAnotherQuestion() {
        passQuiz(STUDENT, Map.of());

        assertThatThrownBy(() -> practice.submit(STUDENT, Map.of(b.id(), right(c))))
                .isInstanceOf(ValidationException.class);
    }

    // ---------- helpers ----------

    private void passQuiz(long studentId, Map<Long, Long> answers) {
        Attempt attempt = attemptService.start(studentId, quiz.id());
        attemptService.submit(studentId, attempt.id(), new HashMap<>(answers));
    }

    private static Long right(Question question) {
        return option(question, true);
    }

    private static Long wrong(Question question) {
        return option(question, false);
    }

    private static Long option(Question question, boolean correct) {
        return question.options().stream()
                .filter(option -> option.correct() == correct)
                .map(AnswerOption::id)
                .findFirst()
                .orElseThrow();
    }
}
