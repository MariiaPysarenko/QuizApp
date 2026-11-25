package com.quizapp.common;

/**
 * Користувач не має права на цю дію (HTTP 403).
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
