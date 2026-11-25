package com.quizapp.attempt.scoring;

import com.quizapp.domain.AnswerOption;
import com.quizapp.domain.Question;
import com.quizapp.quiz.QuizBuilder;

/**
 * Допоміжні методи для тестів оцінювання.
 */
final class ScoringTestData {

    private ScoringTestData() {
    }

    static Question question(int points) {
        return new QuizBuilder()
                .title("Тестовий квіз")
                .question("Запитання на " + points + " б.")
                .correct("правильно")
                .wrong("неправильно")
                .points(points)
                .build()
                .questions()
                .get(0);
    }

    static GivenAnswer correct(Question question) {
        return new GivenAnswer(question, option(question, true));
    }

    static GivenAnswer wrong(Question question) {
        return new GivenAnswer(question, option(question, false));
    }

    static GivenAnswer skipped(Question question) {
        return new GivenAnswer(question, null);
    }

    private static AnswerOption option(Question question, boolean correct) {
        return question.options().stream()
                .filter(option -> option.correct() == correct)
                .findFirst()
                .orElseThrow();
    }
}
