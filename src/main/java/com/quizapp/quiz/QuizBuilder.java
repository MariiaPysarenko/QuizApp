package com.quizapp.quiz;

import com.quizapp.domain.AnswerOption;
import com.quizapp.domain.Question;
import com.quizapp.domain.Quiz;
import com.quizapp.domain.QuizStatus;
import com.quizapp.domain.ScoringMode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Патерн Builder (породжувальний).
 * <p>
 * Квіз складається з багатьох частин, тому збирається покроково:
 * {@code question(...)} починає нове запитання, а {@code correct/wrong/points/explanation}
 * стосуються останнього доданого запитання. Усі правила перевіряються один раз у {@link #build()},
 * тому некоректний {@link Quiz} неможливо отримати ні при ручному створенні, ні при імпорті.
 */
public class QuizBuilder {

    private static final int DEFAULT_POINTS = 1;
    private static final int MIN_OPTIONS = 2;

    private Long creatorId;
    private String title;
    private String description;
    private ScoringMode scoringMode = ScoringMode.WEIGHTED;
    private Duration timeLimit;
    private QuizStatus status = QuizStatus.DRAFT;
    private final List<QuestionDraft> questions = new ArrayList<>();

    public QuizBuilder title(String title) {
        this.title = title;
        return this;
    }

    public QuizBuilder description(String description) {
        this.description = description;
        return this;
    }

    public QuizBuilder creatorId(long creatorId) {
        this.creatorId = creatorId;
        return this;
    }

    public QuizBuilder scoringMode(ScoringMode scoringMode) {
        this.scoringMode = scoringMode;
        return this;
    }

    public QuizBuilder timeLimit(Duration timeLimit) {
        this.timeLimit = timeLimit;
        return this;
    }

    public QuizBuilder status(QuizStatus status) {
        this.status = status;
        return this;
    }

    public QuizBuilder question(String text) {
        questions.add(new QuestionDraft(text));
        return this;
    }

    public QuizBuilder option(String text, boolean correct) {
        currentQuestion().options.add(new AnswerOption(null, text, correct));
        return this;
    }

    public QuizBuilder correct(String text) {
        return option(text, true);
    }

    public QuizBuilder wrong(String text) {
        return option(text, false);
    }

    public QuizBuilder points(int points) {
        currentQuestion().points = points;
        return this;
    }

    public QuizBuilder explanation(String explanation) {
        currentQuestion().explanation = explanation;
        return this;
    }

    public Quiz build() {
        if (isBlank(title)) {
            throw new InvalidQuizException("Квіз повинен мати назву");
        }
        if (questions.isEmpty()) {
            throw new InvalidQuizException("Квіз повинен містити хоча б одне запитання");
        }
        if (timeLimit != null && (timeLimit.isZero() || timeLimit.isNegative())) {
            throw new InvalidQuizException("Обмеження часу має бути додатним");
        }
        List<Question> built = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            built.add(questions.get(i).build(i + 1));
        }
        return new Quiz(null, creatorId, title.strip(), description, scoringMode, timeLimit, status, built);
    }

    private QuestionDraft currentQuestion() {
        if (questions.isEmpty()) {
            throw new IllegalStateException("Спочатку додайте запитання через question(...)");
        }
        return questions.getLast();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Чернетка запитання, яку можна змінювати під час побудови.
     * Перетворюється на незмінний {@link Question} лише після перевірки.
     */
    private static final class QuestionDraft {
        private final String text;
        private final List<AnswerOption> options = new ArrayList<>();
        private int points = DEFAULT_POINTS;
        private String explanation;

        private QuestionDraft(String text) {
            this.text = text;
        }

        private Question build(int number) {
            if (isBlank(text)) {
                throw invalid(number, "текст не може бути порожнім");
            }
            if (points <= 0) {
                throw invalid(number, "кількість балів має бути більшою за 0");
            }
            if (options.size() < MIN_OPTIONS) {
                throw invalid(number, "потрібно щонайменше " + MIN_OPTIONS + " варіанти відповіді");
            }
            long correctCount = options.stream().filter(AnswerOption::correct).count();
            if (correctCount != 1) {
                throw invalid(number, "має бути рівно одна правильна відповідь, а знайдено " + correctCount);
            }
            return new Question(null, text.strip(), points, explanation, options);
        }

        private static InvalidQuizException invalid(int number, String reason) {
            return new InvalidQuizException("Запитання " + number + ": " + reason);
        }
    }
}
