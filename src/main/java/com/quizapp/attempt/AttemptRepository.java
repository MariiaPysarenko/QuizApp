package com.quizapp.attempt;

import com.quizapp.domain.Attempt;

import java.util.Optional;

/**
 * Патерн Repository: доступ до спроб проходження.
 */
public interface AttemptRepository {

    /** Зберігає нову спробу (призначає id) або оновлює наявну. */
    Attempt save(Attempt attempt);

    Optional<Attempt> findById(long id);
}
