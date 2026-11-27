package com.quizapp.persistence.memory;

import com.quizapp.domain.AnswerOption;
import com.quizapp.domain.Question;
import com.quizapp.domain.Quiz;
import com.quizapp.quiz.QuizRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Зберігання квізів у пам'яті — для тестів і для запуску застосунку до підключення бази даних.
 */
public class InMemoryQuizRepository implements QuizRepository {

    private final Map<Long, Quiz> quizzes = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public Quiz save(Quiz quiz) {
        Quiz stored = quiz.id() == null ? withGeneratedIds(quiz) : quiz;
        quizzes.put(stored.id(), stored);
        return stored;
    }

    @Override
    public Optional<Quiz> findById(long id) {
        return Optional.ofNullable(quizzes.get(id));
    }

    @Override
    public List<Quiz> findAll() {
        return quizzes.values().stream()
                .sorted(Comparator.comparing(Quiz::id))
                .toList();
    }

    @Override
    public Optional<Question> findQuestionById(long questionId) {
        return quizzes.values().stream()
                .flatMap(quiz -> quiz.questions().stream())
                .filter(question -> question.id() == questionId)
                .findFirst();
    }

    private Quiz withGeneratedIds(Quiz quiz) {
        List<Question> questions = quiz.questions().stream()
                .map(this::withGeneratedIds)
                .toList();
        return new Quiz(sequence.incrementAndGet(), quiz.creatorId(), quiz.title(), quiz.description(),
                quiz.scoringMode(), quiz.timeLimit(), quiz.status(), questions);
    }

    private Question withGeneratedIds(Question question) {
        List<AnswerOption> options = question.options().stream()
                .map(option -> new AnswerOption(sequence.incrementAndGet(), option.text(), option.correct()))
                .toList();
        return new Question(sequence.incrementAndGet(), question.text(), question.points(),
                question.explanation(), options);
    }
}
