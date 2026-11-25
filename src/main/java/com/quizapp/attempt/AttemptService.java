package com.quizapp.attempt;

import com.quizapp.attempt.scoring.GivenAnswer;
import com.quizapp.attempt.scoring.ScoringResult;
import com.quizapp.attempt.scoring.ScoringStrategies;
import com.quizapp.common.AccessDeniedException;
import com.quizapp.common.ConflictException;
import com.quizapp.common.NotFoundException;
import com.quizapp.common.ValidationException;
import com.quizapp.domain.AnswerOption;
import com.quizapp.domain.Attempt;
import com.quizapp.domain.Question;
import com.quizapp.domain.Quiz;
import com.quizapp.domain.SubmittedAnswer;
import com.quizapp.quiz.QuizRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Проходження квізу: початок спроби, перевірка та оцінювання відповідей.
 * <p>
 * Час береться з {@link Clock}, а не з {@code Instant.now()}, щоб у тестах
 * можна було «перемотати» час і перевірити обмеження без очікування.
 */
public class AttemptService {

    /** Запас часу на мережеву затримку при надсиланні відповідей. */
    static final Duration GRACE_PERIOD = Duration.ofSeconds(30);

    private final QuizRepository quizzes;
    private final AttemptRepository attempts;
    private final Clock clock;

    public AttemptService(QuizRepository quizzes, AttemptRepository attempts, Clock clock) {
        this.quizzes = quizzes;
        this.attempts = attempts;
        this.clock = clock;
    }

    public Attempt start(long studentId, long quizId) {
        Quiz quiz = quizzes.findById(quizId)
                .filter(Quiz::isPublished)
                .orElseThrow(() -> new NotFoundException("Квіз не знайдено"));
        return attempts.save(Attempt.startQuiz(studentId, quiz.id(), clock.instant()));
    }

    /**
     * Перевіряє та оцінює відповіді.
     *
     * @param selectedOptions id запитання → id обраного варіанта; відсутнє запитання вважається пропущеним
     */
    public ScoringResult submit(long studentId, long attemptId, Map<Long, Long> selectedOptions) {
        Attempt attempt = attempts.findById(attemptId)
                .orElseThrow(() -> new NotFoundException("Спробу не знайдено"));
        if (!attempt.belongsTo(studentId)) {
            throw new AccessDeniedException("Це спроба іншого користувача");
        }
        if (!attempt.isInProgress()) {
            throw new ConflictException("Спроба вже завершена");
        }
        Quiz quiz = quizzes.findById(attempt.quizId())
                .orElseThrow(() -> new NotFoundException("Квіз не знайдено"));

        Instant now = clock.instant();
        if (isTimeOver(quiz, attempt, now)) {
            attempts.save(attempt.expire(now));
            throw new ConflictException("Час на проходження квізу вичерпано");
        }

        List<GivenAnswer> givenAnswers = toGivenAnswers(quiz, selectedOptions);
        ScoringResult result = ScoringStrategies.forMode(quiz.scoringMode()).score(givenAnswers);

        List<SubmittedAnswer> submitted = result.questions().stream()
                .map(r -> new SubmittedAnswer(
                        r.question().id(),
                        r.selected() == null ? null : r.selected().id(),
                        r.correct()))
                .toList();
        attempts.save(attempt.complete(result.score(), result.maxScore(), submitted, now));
        return result;
    }

    private static boolean isTimeOver(Quiz quiz, Attempt attempt, Instant now) {
        if (quiz.timeLimit() == null) {
            return false;
        }
        Instant deadline = attempt.startedAt().plus(quiz.timeLimit()).plus(GRACE_PERIOD);
        return now.isAfter(deadline);
    }

    private static List<GivenAnswer> toGivenAnswers(Quiz quiz, Map<Long, Long> selectedOptions) {
        Set<Long> questionIds = quiz.questions().stream()
                .map(Question::id)
                .collect(Collectors.toSet());
        for (Long questionId : selectedOptions.keySet()) {
            if (!questionIds.contains(questionId)) {
                throw new ValidationException("Запитання " + questionId + " не належить до цього квізу");
            }
        }

        List<GivenAnswer> result = new ArrayList<>();
        for (Question question : quiz.questions()) {
            Long optionId = selectedOptions.get(question.id());
            AnswerOption selected = optionId == null ? null : findOption(question, optionId);
            result.add(new GivenAnswer(question, selected));
        }
        return result;
    }

    private static AnswerOption findOption(Question question, Long optionId) {
        return question.options().stream()
                .filter(option -> Objects.equals(option.id(), optionId))
                .findFirst()
                .orElseThrow(() -> new ValidationException(
                        "Варіант " + optionId + " не належить до запитання " + question.id()));
    }
}
