package com.quizapp.web.http;

import com.quizapp.web.ApiSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.Optional;

/**
 * Патерн Adapter: пристосовує {@link HttpSession} сервлета до інтерфейсу {@link ApiSession}.
 */
final class ServletSession implements ApiSession {

    private static final String USER_ID = "userId";

    private final HttpServletRequest request;

    ServletSession(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public Optional<Long> userId() {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }
        Object userId = session.getAttribute(USER_ID);
        return userId instanceof Long id ? Optional.of(id) : Optional.empty();
    }

    @Override
    public void login(long userId) {
        // Нова сесія після входу захищає від атаки фіксації сесії (session fixation).
        HttpSession old = request.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        request.getSession(true).setAttribute(USER_ID, userId);
    }

    @Override
    public void logout() {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
