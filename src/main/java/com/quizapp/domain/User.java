package com.quizapp.domain;

import java.time.Instant;

/**
 * Зареєстрований користувач.
 *
 * @param id           ідентифікатор; {@code null}, доки користувача не збережено
 * @param email        email у нижньому регістрі — використовується як логін
 * @param passwordHash хеш пароля; сам пароль ніде не зберігається
 */
public record User(Long id, String email, String passwordHash, Role role, Instant createdAt) {
}
