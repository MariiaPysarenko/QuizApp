package com.quizapp.quiz;

import com.quizapp.domain.Quiz;

/**
 * Патерн Builder (породжувальний): покрокове створення квізу з перевіркою в build().
 * Поки що лише заглушка — тести QuizBuilderTest мають падати.
 */
public class QuizBuilder {

    public QuizBuilder title(String title) {
        return this;
    }

    public QuizBuilder description(String description) {
        return this;
    }

    public QuizBuilder question(String text) {
        return this;
    }

    public QuizBuilder option(String text, boolean correct) {
        return this;
    }

    public QuizBuilder correct(String text) {
        return this;
    }

    public QuizBuilder wrong(String text) {
        return this;
    }

    public QuizBuilder points(int points) {
        return this;
    }

    public QuizBuilder explanation(String explanation) {
        return this;
    }

    public Quiz build() {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }
}
