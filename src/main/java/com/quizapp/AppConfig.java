package com.quizapp;

/**
 * Налаштування застосунку зі змінних середовища.
 *
 * @param port              порт HTTP-сервера ({@code PORT}, за замовчуванням 8080)
 * @param teacherInviteCode код для реєстрації викладачів ({@code TEACHER_INVITE_CODE});
 *                          якщо не задано, реєстрацію викладачів вимкнено
 */
public record AppConfig(int port, String teacherInviteCode) {

    static final int DEFAULT_PORT = 8080;

    public static AppConfig fromEnvironment() {
        String port = System.getenv("PORT");
        return new AppConfig(
                port == null || port.isBlank() ? DEFAULT_PORT : Integer.parseInt(port.strip()),
                System.getenv("TEACHER_INVITE_CODE"));
    }
}
