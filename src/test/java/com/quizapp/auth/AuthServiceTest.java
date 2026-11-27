package com.quizapp.auth;

import com.quizapp.common.AccessDeniedException;
import com.quizapp.common.ConflictException;
import com.quizapp.common.UnauthorizedException;
import com.quizapp.common.ValidationException;
import com.quizapp.domain.Role;
import com.quizapp.domain.User;
import com.quizapp.persistence.memory.InMemoryUserRepository;
import com.quizapp.testsupport.TestClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {

    private static final String INVITE_CODE = "teacher-2026";
    private static final String PASSWORD = "secret-password";
    private static final Instant NOW = Instant.parse("2026-11-28T10:00:00Z");

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final PasswordHasher hasher = new Pbkdf2PasswordHasher(1_000);
    private final AuthService auth = new AuthService(users, hasher, INVITE_CODE, new TestClock(NOW));

    // ---------- register ----------

    @Test
    @DisplayName("Студент реєструється, пароль зберігається лише як хеш")
    void registersStudent() {
        User user = auth.register("maria@example.com", PASSWORD, Role.STUDENT, null);

        assertThat(user.id()).isNotNull();
        assertThat(user.email()).isEqualTo("maria@example.com");
        assertThat(user.role()).isEqualTo(Role.STUDENT);
        assertThat(user.createdAt()).isEqualTo(NOW);
        assertThat(user.passwordHash().contains(PASSWORD)).isFalse();
        assertThat(hasher.matches(PASSWORD, user.passwordHash())).isTrue();
        assertThat(users.findByEmail("maria@example.com").isPresent()).isTrue();
    }

    @Test
    @DisplayName("Email зберігається без пробілів і в нижньому регістрі")
    void normalizesEmail() {
        User user = auth.register("  Maria@Example.COM ", PASSWORD, Role.STUDENT, null);

        assertThat(user.email()).isEqualTo("maria@example.com");
    }

    @Test
    @DisplayName("Не можна зареєструвати той самий email двічі, незалежно від регістру")
    void rejectsDuplicateEmail() {
        auth.register("maria@example.com", PASSWORD, Role.STUDENT, null);

        assertThatThrownBy(() -> auth.register("MARIA@example.com", PASSWORD, Role.STUDENT, null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Некоректний email відхиляється")
    void rejectsInvalidEmail() {
        assertThatThrownBy(() -> auth.register("maria.example.com", PASSWORD, Role.STUDENT, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("email");
    }

    @Test
    @DisplayName("Пароль коротший за 8 символів відхиляється")
    void rejectsShortPassword() {
        assertThatThrownBy(() -> auth.register("maria@example.com", "1234567", Role.STUDENT, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Пароль");
    }

    @Test
    @DisplayName("Без ролі зареєструватися не можна")
    void rejectsMissingRole() {
        assertThatThrownBy(() -> auth.register("maria@example.com", PASSWORD, null, null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("Викладач реєструється з правильним кодом запрошення")
    void registersTeacherWithInviteCode() {
        User teacher = auth.register("teacher@example.com", PASSWORD, Role.TEACHER, INVITE_CODE);

        assertThat(teacher.role()).isEqualTo(Role.TEACHER);
    }

    @Test
    @DisplayName("Без правильного коду запрошення викладачем стати не можна")
    void rejectsTeacherWithWrongInviteCode() {
        assertThatThrownBy(() -> auth.register("teacher@example.com", PASSWORD, Role.TEACHER, "wrong"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> auth.register("teacher@example.com", PASSWORD, Role.TEACHER, null))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(users.findByEmail("teacher@example.com").isPresent()).isFalse();
    }

    @Test
    @DisplayName("Якщо код запрошення не налаштовано, реєстрацію викладачів вимкнено")
    void teacherRegistrationDisabledWithoutConfiguredCode() {
        AuthService withoutCode = new AuthService(users, hasher, null, new TestClock(NOW));

        assertThatThrownBy(() -> withoutCode.register("teacher@example.com", PASSWORD, Role.TEACHER, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---------- login ----------

    @Test
    @DisplayName("Вхід з правильним паролем повертає користувача")
    void logsInWithCorrectPassword() {
        User registered = auth.register("maria@example.com", PASSWORD, Role.STUDENT, null);

        User loggedIn = auth.login(" MARIA@example.com", PASSWORD);

        assertThat(loggedIn.id()).isEqualTo(registered.id());
    }

    @Test
    @DisplayName("Невірний пароль і невідомий email дають однакову помилку")
    void sameErrorForWrongPasswordAndUnknownEmail() {
        auth.register("maria@example.com", PASSWORD, Role.STUDENT, null);

        assertThatThrownBy(() -> auth.login("maria@example.com", "wrong-password"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Невірний email або пароль");
        assertThatThrownBy(() -> auth.login("nobody@example.com", PASSWORD))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Невірний email або пароль");
    }
}
