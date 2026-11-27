package com.quizapp.auth;

import com.quizapp.common.AccessDeniedException;
import com.quizapp.common.ConflictException;
import com.quizapp.common.UnauthorizedException;
import com.quizapp.common.ValidationException;
import com.quizapp.domain.Role;
import com.quizapp.domain.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Реєстрація та вхід користувачів.
 * <p>
 * Роль викладача можна отримати лише з кодом запрошення, який задає адміністратор сервера.
 * Інакше будь-який студент міг би зареєструватися викладачем.
 */
public class AuthService {

    static final int MIN_PASSWORD_LENGTH = 8;
    static final String INVALID_CREDENTIALS = "Невірний email або пароль";

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final String teacherInviteCode;
    private final Clock clock;

    /**
     * @param teacherInviteCode код для реєстрації викладачів; {@code null} або порожній — реєстрацію викладачів вимкнено
     */
    public AuthService(UserRepository users, PasswordHasher hasher, String teacherInviteCode, Clock clock) {
        this.users = users;
        this.hasher = hasher;
        this.teacherInviteCode = teacherInviteCode;
        this.clock = clock;
    }

    public User register(String email, String password, Role role, String inviteCode) {
        String normalizedEmail = normalize(email);
        if (!EMAIL.matcher(normalizedEmail).matches()) {
            throw new ValidationException("Некоректний email");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Пароль має містити щонайменше " + MIN_PASSWORD_LENGTH + " символів");
        }
        if (role == null) {
            throw new ValidationException("Не вказано роль");
        }
        if (role == Role.TEACHER && !isValidInviteCode(inviteCode)) {
            throw new AccessDeniedException("Для реєстрації викладача потрібен правильний код запрошення");
        }
        if (users.findByEmail(normalizedEmail).isPresent()) {
            throw new ConflictException("Користувач з таким email уже існує");
        }
        return users.save(new User(null, normalizedEmail, hasher.hash(password), role, clock.instant()));
    }

    /** Повертає користувача; для невідомого email і невірного пароля помилка однакова. */
    public User login(String email, String password) {
        return users.findByEmail(normalize(email))
                .filter(user -> hasher.matches(password, user.passwordHash()))
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));
    }

    private boolean isValidInviteCode(String inviteCode) {
        if (teacherInviteCode == null || teacherInviteCode.isBlank() || inviteCode == null) {
            return false;
        }
        return MessageDigest.isEqual(
                teacherInviteCode.getBytes(StandardCharsets.UTF_8),
                inviteCode.getBytes(StandardCharsets.UTF_8));
    }

    private static String normalize(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }
}
