package com.quizapp.common;

/**
 * Користувач не автентифікований або ввів невірні облікові дані (HTTP 401).
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
