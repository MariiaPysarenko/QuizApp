package com.quizapp.quiz;

import com.quizapp.common.AccessDeniedException;
import com.quizapp.common.ConflictException;
import com.quizapp.common.NotFoundException;
import com.quizapp.domain.Quiz;
import com.quizapp.domain.QuizStatus;
import com.quizapp.domain.Role;
import com.quizapp.domain.User;

import java.util.List;
import java.util.Optional;

/**
 * Керування квізами: створення, перегляд з урахуванням ролі, публікація.
 * <p>
 * Правило видимості: студенти бачать лише опубліковані квізи,
 * викладач — опубліковані та власні чернетки.
 */
public class QuizService {

    private final QuizRepository quizzes;

    public QuizService(QuizRepository quizzes) {
        this.quizzes = quizzes;
    }

    /** Створює квіз-чернетку викладача; автора й статус задає сервіс, а не клієнт. */
    public Quiz create(long teacherId, QuizBuilder builder) {
        Quiz quiz = builder
                .creatorId(teacherId)
                .status(QuizStatus.DRAFT)
                .build();
        return quizzes.save(quiz);
    }

    public List<Quiz> list(User user) {
        return quizzes.findAll().stream()
                .filter(quiz -> isVisibleTo(quiz, user))
                .toList();
    }

    public Quiz get(User user, long quizId) {
        return quizzes.findById(quizId)
                .filter(quiz -> isVisibleTo(quiz, user))
                .orElseThrow(() -> new NotFoundException("Квіз не знайдено"));
    }

    public Optional<Quiz> find(long quizId) {
        return quizzes.findById(quizId);
    }

    public Quiz publish(long teacherId, long quizId) {
        Quiz quiz = quizzes.findById(quizId)
                .orElseThrow(() -> new NotFoundException("Квіз не знайдено"));
        if (!quiz.isCreatedBy(teacherId)) {
            throw new AccessDeniedException("Опублікувати квіз може лише його автор");
        }
        if (quiz.status() == QuizStatus.ARCHIVED) {
            throw new ConflictException("Архівний квіз не можна опублікувати");
        }
        return quizzes.save(quiz.withStatus(QuizStatus.PUBLISHED));
    }

    private static boolean isVisibleTo(Quiz quiz, User user) {
        if (quiz.isPublished()) {
            return true;
        }
        return user.role() == Role.TEACHER
                && quiz.status() == QuizStatus.DRAFT
                && quiz.isCreatedBy(user.id());
    }
}
