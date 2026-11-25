package com.quizapp.domain;

import java.time.Instant;
import java.util.List;

/**
 * Спроба — одне проходження квізу студентом.
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
}
