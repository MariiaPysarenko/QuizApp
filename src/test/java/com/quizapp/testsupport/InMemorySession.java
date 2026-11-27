package com.quizapp.testsupport;

import com.quizapp.web.ApiSession;

import java.util.Optional;

/**
 * Сесія для тестів — замість {@code HttpSession}.
 */
public final class InMemorySession implements ApiSession {

    private Long userId;

    @Override
    public Optional<Long> userId() {
        return Optional.ofNullable(userId);
    }

    @Override
    public void login(long userId) {
        this.userId = userId;
    }

    @Override
    public void logout() {
        this.userId = null;
    }
}
