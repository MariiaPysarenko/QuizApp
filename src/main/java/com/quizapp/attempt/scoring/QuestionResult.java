package com.quizapp.attempt.scoring;

import com.quizapp.domain.AnswerOption;
import com.quizapp.domain.Question;

/**
 * Результат оцінювання одного запитання.
 *
 * @param points нараховані бали (можуть бути від'ємними у стратегії зі штрафом)
 */
public record QuestionResult(Question question, AnswerOption selected, boolean correct, double points) {
}
