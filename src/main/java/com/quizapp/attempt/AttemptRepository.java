package com.quizapp.attempt;

import com.quizapp.domain.Attempt;

import java.util.List;
import java.util.Optional;

/**
 * Патерн Repository: доступ до спроб проходження.
 */
public interface AttemptRepository {

    /** Зберігає нову спробу (призначає id) або оновлює наявну. */
    Attempt save(Attempt attempt);

    Optional<Attempt> findById(long id);

    /** Завершені спроби користувача (квізи й практика) у порядку завершення — від найстарішої. */
    List<Attempt> findCompletedByUser(long userId);
}
