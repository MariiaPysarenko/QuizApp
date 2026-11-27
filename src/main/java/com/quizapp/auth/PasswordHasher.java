package com.quizapp.auth;

/**
 * Хешування паролів. Інтерфейс дає змогу замінити алгоритм, не змінюючи {@link AuthService}.
 */
public interface PasswordHasher {

    /** Повертає рядок для збереження в базі: алгоритм, параметри, сіль і хеш. */
    String hash(String password);

    /** Чи відповідає пароль збереженому хешу. Для пошкодженого хешу повертає {@code false}. */
    boolean matches(String password, String storedHash);
}
