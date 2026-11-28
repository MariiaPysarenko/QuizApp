package com.quizapp;

/**
 * Налаштування застосунку зі змінних середовища.
 *
 * @param port              порт HTTP-сервера ({@code PORT}, за замовчуванням 8080)
 * @param teacherInviteCode код для реєстрації викладачів ({@code TEACHER_INVITE_CODE});
 *                          якщо не задано, реєстрацію викладачів вимкнено
 * @param dbUrl             JDBC-адреса PostgreSQL ({@code DB_URL}); якщо не задано, дані зберігаються в пам'яті
 * @param dbUser            користувач бази ({@code DB_USER})
 * @param dbPassword        пароль бази ({@code DB_PASSWORD})
 */
public record AppConfig(int port, String teacherInviteCode, String dbUrl, String dbUser, String dbPassword) {

    static final int DEFAULT_PORT = 8080;

    public static AppConfig fromEnvironment() {
        String port = System.getenv("PORT");
        return new AppConfig(
                port == null || port.isBlank() ? DEFAULT_PORT : Integer.parseInt(port.strip()),
                System.getenv("TEACHER_INVITE_CODE"),
                System.getenv("DB_URL"),
                System.getenv("DB_USER"),
                System.getenv("DB_PASSWORD"));
    }

    public boolean usesDatabase() {
        return dbUrl != null && !dbUrl.isBlank();
    }
}
