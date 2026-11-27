package com.quizapp.web;

import com.quizapp.attempt.AttemptRepository;
import com.quizapp.attempt.AttemptService;
import com.quizapp.auth.AuthService;
import com.quizapp.auth.PasswordHasher;
import com.quizapp.auth.UserRepository;
import com.quizapp.importing.QuizImportService;
import com.quizapp.persistence.memory.InMemoryAttemptRepository;
import com.quizapp.persistence.memory.InMemoryQuizRepository;
import com.quizapp.persistence.memory.InMemoryUserRepository;
import com.quizapp.practice.PracticeService;
import com.quizapp.quiz.QuizRepository;
import com.quizapp.quiz.QuizService;
import com.quizapp.web.controller.AttemptController;
import com.quizapp.web.controller.AuthController;
import com.quizapp.web.controller.PracticeController;
import com.quizapp.web.controller.QuizController;

import java.time.Clock;

/**
 * Точка збирання застосунку (Composition Root): тут і лише тут створюються
 * репозиторії, сервіси й контролери та з'єднуються між собою.
 * <p>
 * Щоб перейти з пам'яті на PostgreSQL, достатньо передати в {@link #create} інші репозиторії.
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
        QuizService quizService = new QuizService(quizRepository);
        AttemptService attemptService = new AttemptService(quizRepository, attemptRepository, clock);
        PracticeService practiceService = new PracticeService(quizRepository, attemptRepository, clock);
        QuizImportService importService = new QuizImportService(quizRepository);
        AuthService authService = new AuthService(userRepository, hasher, teacherInviteCode, clock);
        CurrentUser currentUser = new CurrentUser(userRepository);

        Router router = new Router();
        new AuthController(authService, currentUser).routes(router);
        new QuizController(quizService, importService, currentUser).routes(router);
        new AttemptController(attemptService, quizService, currentUser).routes(router);
        new PracticeController(practiceService, currentUser).routes(router);
        return new QuizApp(router);
    }

    /** Усі дані в пам'яті — для тестів і запуску без бази даних. */
    public static QuizApp inMemory(PasswordHasher hasher, String teacherInviteCode, Clock clock) {
        return create(new InMemoryQuizRepository(), new InMemoryAttemptRepository(), new InMemoryUserRepository(),
                hasher, teacherInviteCode, clock);
    }

    public Router router() {
        return router;
    }
}
