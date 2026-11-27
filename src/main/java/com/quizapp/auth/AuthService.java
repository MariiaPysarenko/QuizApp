package com.quizapp.auth;

import com.quizapp.domain.Role;
import com.quizapp.domain.User;

import java.time.Clock;

/**
 * Реєстрація та вхід користувачів.
 * Поки що заглушка — тести AuthServiceTest мають падати.
 */
public class AuthService {

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final String teacherInviteCode;
    private final Clock clock;

    public AuthService(UserRepository users, PasswordHasher hasher, String teacherInviteCode, Clock clock) {
        this.users = users;
        this.hasher = hasher;
        this.teacherInviteCode = teacherInviteCode;
        this.clock = clock;
    }

    public User register(String email, String password, Role role, String inviteCode) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }

    public User login(String email, String password) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }
}
