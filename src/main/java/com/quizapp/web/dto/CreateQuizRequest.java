package com.quizapp.web.dto;

import com.quizapp.domain.ScoringMode;
import com.quizapp.quiz.QuizBuilder;

import java.time.Duration;
import java.util.List;

/** Тіло запиту POST /api/quizzes. */
public record CreateQuizRequest(String title,
                                String description,
                                ScoringMode scoringMode,
                                Integer timeLimitSeconds,
                                List<QuestionRequest> questions) {

    public record QuestionRequest(String text, Integer points, String explanation, List<OptionRequest> options) {
    }

    public record OptionRequest(String text, boolean correct) {
    }

    /** Переносить дані запиту в {@link QuizBuilder}; перевірку виконує сам Builder. */
    public QuizBuilder toBuilder() {
        QuizBuilder builder = new QuizBuilder()
                .title(title)
                .description(description);
        if (scoringMode != null) {
            builder.scoringMode(scoringMode);
        }
        if (timeLimitSeconds != null) {
            builder.timeLimit(Duration.ofSeconds(timeLimitSeconds));
        }
        for (QuestionRequest question : questions == null ? List.<QuestionRequest>of() : questions) {
            builder.question(question.text());
            for (OptionRequest option : question.options() == null ? List.<OptionRequest>of() : question.options()) {
                builder.option(option.text(), option.correct());
            }
            if (question.points() != null) {
                builder.points(question.points());
            }
            if (question.explanation() != null) {
                builder.explanation(question.explanation());
            }
        }
        return builder;
    }
}
