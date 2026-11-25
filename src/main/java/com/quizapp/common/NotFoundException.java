package com.quizapp.common;

/**
 * Ресурс не знайдено або недоступний користувачу (HTTP 404).
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
