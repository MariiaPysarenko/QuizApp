package com.quizapp.quiz;

import com.quizapp.domain.Quiz;

import java.util.Optional;

/**
 * Патерн Repository: доступ до збережених квізів.
 * Сервіси залежать лише від цього інтерфейсу, тому сховище (пам'ять чи PostgreSQL)
 * можна замінити без змін у бізнес-логіці.
 */
public interface QuizRepository {

    /** Зберігає квіз; новому квізу, його запитанням і варіантам призначаються id. */
    Quiz save(Quiz quiz);

    Optional<Quiz> findById(long id);
}
