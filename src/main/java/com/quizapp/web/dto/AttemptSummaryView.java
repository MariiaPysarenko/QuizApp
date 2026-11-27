package com.quizapp.web.dto;

import com.quizapp.domain.Attempt;
import com.quizapp.domain.AttemptStatus;
import com.quizapp.domain.AttemptType;

import java.time.Instant;

/** Запис в історії спроб студента. */
public record AttemptSummaryView(long id,
                                 AttemptType type,
                                 Long quizId,
                                 String title,
                                 AttemptStatus status,
                                 Instant startedAt,
                                 Instant completedAt,
                                 double score,
                                 double maxScore,
                                 double percent) {

    public static AttemptSummaryView from(Attempt attempt, String title) {
        double percent = attempt.maxScore() == 0 ? 0 : attempt.score() / attempt.maxScore() * 100;
        return new AttemptSummaryView(attempt.id(), attempt.type(), attempt.quizId(), title, attempt.status(),
                attempt.startedAt(), attempt.completedAt(), attempt.score(), attempt.maxScore(),
                ResultView.round(percent));
    }
}
