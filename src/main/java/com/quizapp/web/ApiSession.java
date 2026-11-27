package com.quizapp.web;

import java.util.Optional;

/**
 * Сесія користувача. У застосунку її реалізує {@code HttpSession} сервлета,
 * у тестах — проста реалізація в пам'яті. Так маршрути й контролери не залежать від Tomcat.
 */
public interface ApiSession {

    Optional<Long> userId();

    void login(long userId);

    void logout();
}
