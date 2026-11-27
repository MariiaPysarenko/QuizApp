package com.quizapp.web;

import com.quizapp.attempt.AttemptRepository;
import com.quizapp.auth.PasswordHasher;
import com.quizapp.auth.UserRepository;
import com.quizapp.persistence.memory.InMemoryAttemptRepository;
import com.quizapp.persistence.memory.InMemoryQuizRepository;
import com.quizapp.persistence.memory.InMemoryUserRepository;
import com.quizapp.quiz.QuizRepository;

import java.time.Clock;

/**
 * Точка збирання застосунку (Composition Root).
 * Поки що заглушка без маршрутів — тести QuizAppApiTest мають падати.
 */
public final class QuizApp {

    private final Router router;

    private QuizApp(Router router) {
        this.router = router;
    }

    public static QuizApp create(QuizRepository quizRepository,
                                 AttemptRepository attemptRepository,
                                 UserRepository userRepository,
                                 PasswordHasher hasher,
                                 String teacherInviteCode,
                                 Clock clock) {
        return new QuizApp(new Router());
    }

    public static QuizApp inMemory(PasswordHasher hasher, String teacherInviteCode, Clock clock) {
        return create(new InMemoryQuizRepository(), new InMemoryAttemptRepository(), new InMemoryUserRepository(),
                hasher, teacherInviteCode, clock);
    }

    public Router router() {
        return router;
    }
}
