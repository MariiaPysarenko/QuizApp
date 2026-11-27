package com.quizapp.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Pbkdf2PasswordHasherTest {

    /** Мало ітерацій — щоб тести працювали швидко. У застосунку — 600 000. */
    private final PasswordHasher hasher = new Pbkdf2PasswordHasher(1_000);

    @Test
    @DisplayName("Хеш не містить пароля у відкритому вигляді")
    void hashDoesNotContainPassword() {
        String hash = hasher.hash("secret-password");

        assertThat(hash.startsWith("pbkdf2$1000$")).isTrue();
        assertThat(hash.contains("secret-password")).isFalse();
    }

    @Test
    @DisplayName("Правильний пароль проходить перевірку")
    void matchesCorrectPassword() {
        String hash = hasher.hash("secret-password");

        assertThat(hasher.matches("secret-password", hash)).isTrue();
    }

    @Test
    @DisplayName("Неправильний пароль не проходить перевірку")
    void rejectsWrongPassword() {
        String hash = hasher.hash("secret-password");

        assertThat(hasher.matches("Secret-password", hash)).isFalse();
        assertThat(hasher.matches("", hash)).isFalse();
    }

    @Test
    @DisplayName("Однаковий пароль дає різні хеші завдяки випадковій солі")
    void samePasswordGivesDifferentHashes() {
        assertThat(hasher.hash("secret-password").equals(hasher.hash("secret-password"))).isFalse();
    }

    @Test
    @DisplayName("Пошкоджений або порожній хеш не викликає помилки, а просто не підходить")
    void malformedHashDoesNotMatch() {
        assertThat(hasher.matches("secret-password", "not-a-hash")).isFalse();
        assertThat(hasher.matches("secret-password", "pbkdf2$abc$!!!$???")).isFalse();
        assertThat(hasher.matches("secret-password", null)).isFalse();
    }

    @Test
    @DisplayName("Кількість ітерацій береться зі збереженого хешу")
    void usesIterationsStoredInHash() {
        String oldHash = new Pbkdf2PasswordHasher(500).hash("secret-password");

        assertThat(hasher.matches("secret-password", oldHash)).isTrue();
    }
}
