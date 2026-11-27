package com.quizapp.web;

import com.quizapp.auth.Pbkdf2PasswordHasher;
import com.quizapp.domain.AttemptType;
import com.quizapp.domain.QuizStatus;
import com.quizapp.domain.Role;
import com.quizapp.importing.googleforms.GoogleFormExport;
import com.quizapp.testsupport.InMemorySession;
import com.quizapp.testsupport.TestClock;
import com.quizapp.web.dto.AttemptSummaryView;
import com.quizapp.web.dto.CreateQuizRequest;
import com.quizapp.web.dto.CreateQuizRequest.OptionRequest;
import com.quizapp.web.dto.CreateQuizRequest.QuestionRequest;
import com.quizapp.web.dto.ImportView;
import com.quizapp.web.dto.LoginRequest;
import com.quizapp.web.dto.PracticeView;
import com.quizapp.web.dto.QuizSummaryView;
import com.quizapp.web.dto.QuizView;
import com.quizapp.web.dto.RegisterRequest;
import com.quizapp.web.dto.ResultView;
import com.quizapp.web.dto.StartAttemptView;
import com.quizapp.web.dto.SubmitAnswersRequest;
import com.quizapp.web.dto.SubmitAnswersRequest.AnswerRequest;
import com.quizapp.web.dto.UserView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Наскрізні тести API: запити проходять через маршрутизатор, контролери, сервіси й репозиторії,
 * як у запущеному застосунку, але без HTTP-сервера.
 */
class QuizAppApiTest {

    private static final String INVITE_CODE = "teacher-2026";
    private static final String PASSWORD = "secret-password";

    private final Router router = QuizApp.inMemory(new Pbkdf2PasswordHasher(1_000), INVITE_CODE,
            new TestClock(Instant.parse("2026-11-28T10:00:00Z"))).router();

    private final InMemorySession teacher = new InMemorySession();
    private final InMemorySession student = new InMemorySession();

    // ---------- auth ----------

    @Test
    @DisplayName("Реєстрація, вхід, поточний користувач і вихід")
    void registerLoginMeLogout() {
        ApiResponse registered = call(student, "POST", "/api/auth/register",
                new RegisterRequest("maria@example.com", PASSWORD, Role.STUDENT, null));
        assertThat(registered.status()).isEqualTo(201);

        assertThat(call(student, "GET", "/api/me", null).status()).isEqualTo(401);

        ApiResponse login = call(student, "POST", "/api/auth/login", new LoginRequest("maria@example.com", PASSWORD));
        assertThat(login.status()).isEqualTo(200);

        ApiResponse me = call(student, "GET", "/api/me", null);
        assertThat(me.status()).isEqualTo(200);
        assertThat(((UserView) me.body()).email()).isEqualTo("maria@example.com");

        assertThat(call(student, "POST", "/api/auth/logout", null).status()).isEqualTo(204);
        assertThat(call(student, "GET", "/api/me", null).status()).isEqualTo(401);
    }

    @Test
    @DisplayName("Невірний пароль — 401")
    void wrongPasswordGives401() {
        call(student, "POST", "/api/auth/register", new RegisterRequest("maria@example.com", PASSWORD, Role.STUDENT, null));

        ApiResponse response = call(student, "POST", "/api/auth/login", new LoginRequest("maria@example.com", "wrong-pass"));

        assertThat(response.status()).isEqualTo(401);
    }

    @Test
    @DisplayName("Запит без тіла — 400")
    void missingBodyGives400() {
        assertThat(call(student, "POST", "/api/auth/login", null).status()).isEqualTo(400);
    }

    // ---------- quizzes ----------

    @Test
    @DisplayName("Студент не може створити квіз — 403")
    void studentCannotCreateQuiz() {
        loginAsStudent();

        assertThat(call(student, "POST", "/api/quizzes", sampleQuiz()).status()).isEqualTo(403);
    }

    @Test
    @DisplayName("Некоректний квіз — 400 з поясненням")
    void invalidQuizGives400() {
        loginAsTeacher();
        CreateQuizRequest noCorrect = new CreateQuizRequest("Квіз", null, null, null, List.of(
                new QuestionRequest("2 + 2 = ?", 1, null, List.of(
                        new OptionRequest("3", false), new OptionRequest("5", false)))));

        ApiResponse response = call(teacher, "POST", "/api/quizzes", noCorrect);

        assertThat(response.status()).isEqualTo(400);
        assertThat(((ErrorBody) response.body()).error().contains("правильна")).isTrue();
    }

    @Test
    @DisplayName("Чернетка невидима студенту, після публікації — видима без правильних відповідей")
    void draftIsHiddenUntilPublished() {
        loginAsTeacher();
        loginAsStudent();
        QuizView created = (QuizView) call(teacher, "POST", "/api/quizzes", sampleQuiz()).body();
        assertThat(created.status()).isEqualTo(QuizStatus.DRAFT);
        assertThat(created.questions().get(0).options().get(1).correct()).isEqualTo(true);

        assertThat(call(student, "GET", "/api/quizzes/" + created.id(), null).status()).isEqualTo(404);
        assertThat((List<?>) call(student, "GET", "/api/quizzes", null).body()).isEmpty();

        assertThat(call(teacher, "POST", "/api/quizzes/" + created.id() + "/publish", null).status()).isEqualTo(200);

        QuizView forStudent = (QuizView) call(student, "GET", "/api/quizzes/" + created.id(), null).body();
        assertThat(forStudent.questions().get(0).options().get(1).correct()).isNull();
        assertThat(forStudent.questions().get(0).explanation()).isNull();
    }

    // ---------- attempts ----------

    @Test
    @DisplayName("Повний сценарій: викладач створює й публікує квіз, студент проходить, бачить історію")
    void studentPassesQuiz() {
        long quizId = publishedSampleQuiz();
        loginAsStudent();

        StartAttemptView started = (StartAttemptView) call(student, "POST",
                "/api/quizzes/" + quizId + "/attempts", null).body();
        QuizView.QuestionView first = started.quiz().questions().get(0);
        QuizView.QuestionView second = started.quiz().questions().get(1);
        assertThat(started.deadline()).isEqualTo(started.startedAt().plusSeconds(600));

        ApiResponse submitted = call(student, "POST", "/api/attempts/" + started.attemptId() + "/submit",
                new SubmitAnswersRequest(List.of(
                        new AnswerRequest(first.id(), first.options().get(1).id()),
                        new AnswerRequest(second.id(), second.options().get(1).id()))));

        assertThat(submitted.status()).isEqualTo(200);
        ResultView result = (ResultView) submitted.body();
        assertThat(result.score()).isEqualTo(2);
        assertThat(result.maxScore()).isEqualTo(3);
        assertThat(result.percent()).isEqualTo(66.7);
        assertThat(result.questions().get(1).correct()).isFalse();
        assertThat(result.questions().get(1).correctOptionId()).isEqualTo(second.options().get(0).id());
        assertThat(result.questions().get(1).explanation()).isEqualTo("JVM виконує байт-код");

        @SuppressWarnings("unchecked")
        List<AttemptSummaryView> history = (List<AttemptSummaryView>) call(student, "GET", "/api/me/attempts", null).body();
        assertThat(history).hasSize(1);
        assertThat(history.get(0).title()).isEqualTo("Основи Java");
        assertThat(history.get(0).percent()).isEqualTo(66.7);
    }

    @Test
    @DisplayName("Повторне надсилання — 409, викладач не проходить квізи — 403")
    void resubmitAndRoleChecks() {
        long quizId = publishedSampleQuiz();
        loginAsStudent();
        StartAttemptView started = (StartAttemptView) call(student, "POST",
                "/api/quizzes/" + quizId + "/attempts", null).body();
        String submitPath = "/api/attempts/" + started.attemptId() + "/submit";
        SubmitAnswersRequest empty = new SubmitAnswersRequest(List.of());

        assertThat(call(student, "POST", submitPath, empty).status()).isEqualTo(200);
        assertThat(call(student, "POST", submitPath, empty).status()).isEqualTo(409);
        assertThat(call(teacher, "POST", "/api/quizzes/" + quizId + "/attempts", null).status()).isEqualTo(403);
    }

    // ---------- practice ----------

    @Test
    @DisplayName("Робота над помилками: добірка, надсилання, порожня добірка після виправлення")
    void practiceFlow() {
        long quizId = publishedSampleQuiz();
        loginAsStudent();
        StartAttemptView started = (StartAttemptView) call(student, "POST",
                "/api/quizzes/" + quizId + "/attempts", null).body();
        call(student, "POST", "/api/attempts/" + started.attemptId() + "/submit", new SubmitAnswersRequest(List.of()));

        PracticeView practice = (PracticeView) call(student, "GET", "/api/me/practice", null).body();
        assertThat(practice.questions()).hasSize(2);
        assertThat(practice.questions().get(0).options().get(0).correct()).isNull();

        QuizView.QuestionView first = practice.questions().get(0);
        QuizView.QuestionView second = practice.questions().get(1);
        ResultView result = (ResultView) call(student, "POST", "/api/me/practice/submit",
                new SubmitAnswersRequest(List.of(
                        new AnswerRequest(first.id(), first.options().get(1).id()),
                        new AnswerRequest(second.id(), second.options().get(0).id())))).body();
        assertThat(result.percent()).isEqualTo(100);

        assertThat(((PracticeView) call(student, "GET", "/api/me/practice", null).body()).questions()).isEmpty();
        @SuppressWarnings("unchecked")
        List<AttemptSummaryView> history = (List<AttemptSummaryView>) call(student, "GET", "/api/me/attempts", null).body();
        assertThat(history.get(1).type()).isEqualTo(AttemptType.PRACTICE);
    }

    // ---------- import ----------

    @Test
    @DisplayName("Імпорт із Google Forms створює чернетку та повертає звіт")
    void importsGoogleForm() {
        loginAsTeacher();
        GoogleFormExport export = new GoogleFormExport("google-forms", "form-1", "Основи Java", null, true, List.of(
                new GoogleFormExport.Item("MULTIPLE_CHOICE", "Що виконує байт-код?", 1, null, List.of(
                        new GoogleFormExport.Choice("JVM", true), new GoogleFormExport.Choice("javac", false))),
                new GoogleFormExport.Item("CHECKBOX", "Примітивні типи", 2, null, List.of(
                        new GoogleFormExport.Choice("int", true), new GoogleFormExport.Choice("boolean", true))),
                new GoogleFormExport.Item("TEXT", "Прізвище", null, null, null)));

        ApiResponse response = call(teacher, "POST", "/api/quizzes/import/google-forms", export);

        assertThat(response.status()).isEqualTo(201);
        ImportView imported = (ImportView) response.body();
        assertThat(imported.importedCount()).isEqualTo(1);
        assertThat(imported.skipped()).hasSize(2);
        assertThat(imported.quiz().status()).isEqualTo(QuizStatus.DRAFT);
        @SuppressWarnings("unchecked")
        List<QuizSummaryView> teacherQuizzes = (List<QuizSummaryView>) call(teacher, "GET", "/api/quizzes", null).body();
        assertThat(teacherQuizzes).hasSize(1);
    }

    @Test
    @DisplayName("Студент не може імпортувати квіз — 403")
    void studentCannotImport() {
        loginAsStudent();
        GoogleFormExport export = new GoogleFormExport("google-forms", "f", "Форма", null, true, List.of());

        assertThat(call(student, "POST", "/api/quizzes/import/google-forms", export).status()).isEqualTo(403);
    }

    // ---------- helpers ----------

    private ApiResponse call(InMemorySession session, String method, String path, Object body) {
        return router.dispatch(method, path, BodyReader.of(body), session);
    }

    private void loginAsTeacher() {
        call(teacher, "POST", "/api/auth/register",
                new RegisterRequest("teacher@example.com", PASSWORD, Role.TEACHER, INVITE_CODE));
        call(teacher, "POST", "/api/auth/login", new LoginRequest("teacher@example.com", PASSWORD));
    }

    private void loginAsStudent() {
        call(student, "POST", "/api/auth/register", new RegisterRequest("maria@example.com", PASSWORD, Role.STUDENT, null));
        call(student, "POST", "/api/auth/login", new LoginRequest("maria@example.com", PASSWORD));
    }

    /** Квіз «Основи Java»: запитання на 2 бали (правильний — другий варіант) і на 1 бал (правильний — перший). */
    private static CreateQuizRequest sampleQuiz() {
        return new CreateQuizRequest("Основи Java", "ООП", null, 600, List.of(
                new QuestionRequest("Який модифікатор робить поле видимим лише в класі?", 2, "Інкапсуляція", List.of(
                        new OptionRequest("public", false), new OptionRequest("private", true))),
                new QuestionRequest("Що виконує байт-код?", 1, "JVM виконує байт-код", List.of(
                        new OptionRequest("JVM", true), new OptionRequest("javac", false)))));
    }

    private long publishedSampleQuiz() {
        loginAsTeacher();
        QuizView created = (QuizView) call(teacher, "POST", "/api/quizzes", sampleQuiz()).body();
        call(teacher, "POST", "/api/quizzes/" + created.id() + "/publish", null);
        return created.id();
    }
}
