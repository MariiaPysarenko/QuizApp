package com.quizapp.common;

/**
 * Дію не можна виконати в поточному стані ресурсу (HTTP 409).
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
