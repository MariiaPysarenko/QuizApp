package com.quizapp.web.dto;

import java.time.Instant;

/**
 * Відповідь на початок спроби: id спроби, час завершення та запитання без правильних відповідей.
 *
 * @param deadline коли закінчується час; {@code null}, якщо обмеження немає
 */
public record StartAttemptView(long attemptId, Instant startedAt, Instant deadline, QuizView quiz) {
}
