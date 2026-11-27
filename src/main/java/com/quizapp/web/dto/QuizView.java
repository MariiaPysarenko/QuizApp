package com.quizapp.web.dto;

import com.quizapp.domain.AnswerOption;
import com.quizapp.domain.Question;
import com.quizapp.domain.Quiz;
import com.quizapp.domain.QuizStatus;
import com.quizapp.domain.ScoringMode;

import java.util.List;

/**
 * Квіз із запитаннями.
 * Автор бачить правильні відповіді й пояснення; студент — ні (поля {@code null} не потрапляють у JSON).
 */
public record QuizView(long id,
                       String title,
                       String description,
                       QuizStatus status,
                       ScoringMode scoringMode,
                       Long timeLimitSeconds,
                       List<QuestionView> questions) {

    public record QuestionView(long id, String text, int points, String explanation, List<OptionView> options) {
    }

    public record OptionView(long id, String text, Boolean correct) {
    }

    public static QuizView forAuthor(Quiz quiz) {
        return of(quiz, true);
    }

    public static QuizView forStudent(Quiz quiz) {
        return of(quiz, false);
    }

    public static QuestionView question(Question question, boolean withAnswers) {
        List<OptionView> options = question.options().stream()
                .map(option -> option(option, withAnswers))
                .toList();
        return new QuestionView(question.id(), question.text(), question.points(),
                withAnswers ? question.explanation() : null, options);
    }

    static Long seconds(Quiz quiz) {
        return quiz.timeLimit() == null ? null : quiz.timeLimit().toSeconds();
    }

    private static QuizView of(Quiz quiz, boolean withAnswers) {
        List<QuestionView> questions = quiz.questions().stream()
                .map(question -> question(question, withAnswers))
                .toList();
        return new QuizView(quiz.id(), quiz.title(), quiz.description(), quiz.status(),
                quiz.scoringMode(), seconds(quiz), questions);
    }

    private static OptionView option(AnswerOption option, boolean withAnswers) {
        return new OptionView(option.id(), option.text(), withAnswers ? option.correct() : null);
    }
}
