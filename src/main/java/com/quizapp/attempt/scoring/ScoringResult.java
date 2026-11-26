package com.quizapp.attempt.scoring;

import com.quizapp.domain.SubmittedAnswer;

import java.util.List;

/**
 * Підсумок оцінювання спроби.
 *
 * @param questions результати по кожному запитанню
 * @param score     набрані бали
 * @param maxScore  максимально можливі бали
 */
public record ScoringResult(List<QuestionResult> questions, double score, double maxScore) {

    public ScoringResult {
        questions = List.copyOf(questions);
    }

    /** Відсоток від максимуму; 0, якщо запитань немає. */
    public double percent() {
        if (maxScore == 0) {
            return 0;
        }
        return score / maxScore * 100;
    }

    /** Запитання з неправильною або пропущеною відповіддю — основа для роботи над помилками. */
    public List<QuestionResult> mistakes() {
        return questions.stream()
                .filter(result -> !result.correct())
                .toList();
    }

    /** Відповіді у вигляді для збереження в спробі. */
    public List<SubmittedAnswer> toSubmittedAnswers() {
        return questions.stream()
                .map(result -> new SubmittedAnswer(
                        result.question().id(),
                        result.selected() == null ? null : result.selected().id(),
                        result.correct()))
                .toList();
    }
}
