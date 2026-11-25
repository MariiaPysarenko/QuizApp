package com.quizapp.domain;

import java.time.Instant;
import java.util.List;

/**
 * Спроба — одне проходження квізу студентом.
 * Незмінна: кожен перехід стану ({@link #complete}, {@link #expire}) повертає нову спробу.
 *
 * @param id     ідентифікатор; {@code null}, доки спробу не збережено
 * @param quizId квіз; {@code null} для спроби типу {@link AttemptType#PRACTICE}
 */
public record Attempt(Long id,
                      long userId,
                      Long quizId,
                      AttemptType type,
                      AttemptStatus status,
                      Instant startedAt,
                      Instant completedAt,
                      double score,
                      double maxScore,
                      List<SubmittedAnswer> answers) {

    public Attempt {
        answers = List.copyOf(answers);
    }

    public static Attempt startQuiz(long userId, long quizId, Instant now) {
        return new Attempt(null, userId, quizId, AttemptType.QUIZ, AttemptStatus.IN_PROGRESS,
                now, null, 0, 0, List.of());
    }

    public boolean belongsTo(long userId) {
        return this.userId == userId;
    }

    public boolean isInProgress() {
        return status == AttemptStatus.IN_PROGRESS;
    }

    public Attempt complete(double score, double maxScore, List<SubmittedAnswer> answers, Instant now) {
        return new Attempt(id, userId, quizId, type, AttemptStatus.COMPLETED,
                startedAt, now, score, maxScore, answers);
    }

    public Attempt expire(Instant now) {
        return new Attempt(id, userId, quizId, type, AttemptStatus.EXPIRED,
                startedAt, now, 0, 0, List.of());
    }
}
