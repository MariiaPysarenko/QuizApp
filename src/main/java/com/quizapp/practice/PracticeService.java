package com.quizapp.practice;

import com.quizapp.attempt.AttemptRepository;
import com.quizapp.attempt.scoring.GivenAnswer;
import com.quizapp.attempt.scoring.ScoringResult;
import com.quizapp.attempt.scoring.ScoringStrategies;
import com.quizapp.common.ValidationException;
import com.quizapp.domain.AnswerOption;
import com.quizapp.domain.Attempt;
import com.quizapp.domain.Question;
import com.quizapp.domain.ScoringMode;
import com.quizapp.domain.SubmittedAnswer;
import com.quizapp.quiz.QuizRepository;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Робота над помилками: повторне проходження запитань, на які студент востаннє відповів неправильно.
 */
public class PracticeService {

    private final QuizRepository quizzes;
    private final AttemptRepository attempts;
    private final Clock clock;

    public PracticeService(QuizRepository quizzes, AttemptRepository attempts, Clock clock) {
        this.quizzes = quizzes;
        this.attempts = attempts;
        this.clock = clock;
    }

    /** Запитання, остання відповідь студента на які була неправильною або відсутньою. */
    public List<Question> buildPractice(long studentId) {
        return mistakeQuestionIds(studentId).stream()
                .map(quizzes::findQuestionById)
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * Оцінює відповіді практики та зберігає їх як спробу типу PRACTICE.
     *
     * @param selectedOptions id запитання → id обраного варіанта; лише запитання з поточної добірки
     */
    public ScoringResult submit(long studentId, Map<Long, Long> selectedOptions) {
        if (selectedOptions.isEmpty()) {
            throw new ValidationException("Немає відповідей для перевірки");
        }
        Set<Long> allowed = mistakeQuestionIds(studentId);
        List<GivenAnswer> givenAnswers = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : selectedOptions.entrySet()) {
            Long questionId = entry.getKey();
            if (!allowed.contains(questionId)) {
                throw new ValidationException("Запитання " + questionId + " немає у вашій добірці для практики");
            }
            Question question = quizzes.findQuestionById(questionId)
                    .orElseThrow(() -> new ValidationException("Запитання " + questionId + " не знайдено"));
            givenAnswers.add(new GivenAnswer(question, findOption(question, entry.getValue())));
        }

        ScoringResult result = ScoringStrategies.forMode(ScoringMode.WEIGHTED).score(givenAnswers);
        List<SubmittedAnswer> submitted = result.questions().stream()
                .map(r -> new SubmittedAnswer(
                        r.question().id(),
                        r.selected() == null ? null : r.selected().id(),
                        r.correct()))
                .toList();
        attempts.save(Attempt.completedPractice(studentId, result.score(), result.maxScore(),
                submitted, clock.instant()));
        return result;
    }

    /** Остання відповідь на кожне запитання визначає, чи потрібна по ньому практика. */
    private Set<Long> mistakeQuestionIds(long studentId) {
        Map<Long, Boolean> lastAnswerCorrect = new LinkedHashMap<>();
        for (Attempt attempt : attempts.findCompletedByUser(studentId)) {
            for (SubmittedAnswer answer : attempt.answers()) {
                lastAnswerCorrect.put(answer.questionId(), answer.correct());
            }
        }
        lastAnswerCorrect.values().removeIf(correct -> correct);
        return lastAnswerCorrect.keySet();
    }

    private static AnswerOption findOption(Question question, Long optionId) {
        return question.options().stream()
                .filter(option -> Objects.equals(option.id(), optionId))
                .findFirst()
                .orElseThrow(() -> new ValidationException(
                        "Варіант " + optionId + " не належить до запитання " + question.id()));
    }
}
