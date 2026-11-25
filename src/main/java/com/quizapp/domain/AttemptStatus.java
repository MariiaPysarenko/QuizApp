package com.quizapp.domain;

/**
 * Стан спроби.
 */
public enum AttemptStatus {
    IN_PROGRESS,
    COMPLETED,
    /** Відповіді надіслано після завершення часу — вони не зараховані. */
    EXPIRED
}
