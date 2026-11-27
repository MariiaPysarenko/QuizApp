package com.quizapp.auth;

import com.quizapp.domain.User;

import java.util.Optional;

/**
 * Патерн Repository: доступ до користувачів.
 */
public interface UserRepository {

    /** Зберігає нового користувача (призначає id) або оновлює наявного. */
    User save(User user);

    Optional<User> findById(long id);

    /** Пошук за email; email очікується вже в нижньому регістрі. */
    Optional<User> findByEmail(String email);
}
