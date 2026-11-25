package com.quizapp.domain;

/**
 * Стан квізу. Студенти бачать і проходять лише опубліковані квізи.
 */
public enum QuizStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}
