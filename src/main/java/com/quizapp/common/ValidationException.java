package com.quizapp.common;

/**
 * Некоректні вхідні дані (HTTP 400).
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
