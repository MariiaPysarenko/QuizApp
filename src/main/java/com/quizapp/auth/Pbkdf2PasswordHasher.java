package com.quizapp.auth;

/**
 * Хешування паролів алгоритмом PBKDF2WithHmacSHA256 зі стандартної бібліотеки JDK.
 * Поки що заглушка — тести Pbkdf2PasswordHasherTest мають падати.
 */
public class Pbkdf2PasswordHasher implements PasswordHasher {

    public static final int DEFAULT_ITERATIONS = 600_000;

    private final int iterations;

    public Pbkdf2PasswordHasher() {
        this(DEFAULT_ITERATIONS);
    }

    public Pbkdf2PasswordHasher(int iterations) {
        this.iterations = iterations;
    }

    @Override
    public String hash(String password) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }

    @Override
    public boolean matches(String password, String storedHash) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }
}
