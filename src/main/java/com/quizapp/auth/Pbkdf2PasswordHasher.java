package com.quizapp.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Хешування паролів алгоритмом PBKDF2WithHmacSHA256 зі стандартної бібліотеки JDK.
 * <p>
 * Формат збереженого значення: {@code pbkdf2$<ітерації>$<сіль Base64>$<хеш Base64>}.
 * Кількість ітерацій зберігається разом із хешем, тому її можна збільшити в майбутньому,
 * і старі паролі все одно перевірятимуться.
 */
public class Pbkdf2PasswordHasher implements PasswordHasher {

    /** Рекомендація OWASP для PBKDF2-HMAC-SHA256. */
    public static final int DEFAULT_ITERATIONS = 600_000;

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2";
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private final int iterations;
    private final SecureRandom random = new SecureRandom();

    public Pbkdf2PasswordHasher() {
        this(DEFAULT_ITERATIONS);
    }

    /** Менша кількість ітерацій використовується в тестах, щоб вони працювали швидко. */
    public Pbkdf2PasswordHasher(int iterations) {
        if (iterations <= 0) {
            throw new IllegalArgumentException("Кількість ітерацій має бути додатною");
        }
        this.iterations = iterations;
    }

    @Override
    public String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] key = derive(password, salt, iterations);
        Base64.Encoder base64 = Base64.getEncoder();
        return PREFIX + "$" + iterations + "$" + base64.encodeToString(salt) + "$" + base64.encodeToString(key);
    }

    @Override
    public boolean matches(String password, String storedHash) {
        if (password == null || storedHash == null) {
            return false;
        }
        String[] parts = storedHash.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int storedIterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(password, salt, storedIterations);
            // Порівняння за сталий час: не підказує зловмиснику, скільки байтів збіглося.
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Алгоритм " + ALGORITHM + " недоступний", e);
        } finally {
            spec.clearPassword();
        }
    }
}
