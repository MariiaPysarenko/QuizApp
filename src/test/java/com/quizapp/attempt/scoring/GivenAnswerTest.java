package com.quizapp.attempt.scoring;

import com.quizapp.domain.AnswerOption;
import com.quizapp.domain.Question;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.quizapp.attempt.scoring.ScoringTestData.correct;
import static com.quizapp.attempt.scoring.ScoringTestData.question;
import static com.quizapp.attempt.scoring.ScoringTestData.skipped;
import static com.quizapp.attempt.scoring.ScoringTestData.wrong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GivenAnswerTest {

    @Test
    @DisplayName("Розпізнає правильну, неправильну та пропущену відповідь")
    void recognizesAnswerKind() {
        Question question = question(1);

        assertThat(correct(question).isCorrect()).isTrue();
        assertThat(wrong(question).isCorrect()).isFalse();
        assertThat(wrong(question).isSkipped()).isFalse();
        assertThat(skipped(question).isCorrect()).isFalse();
        assertThat(skipped(question).isSkipped()).isTrue();
    }

    @Test
    @DisplayName("Не можна обрати варіант з іншого запитання")
    void rejectsOptionFromAnotherQuestion() {
        Question question = question(1);
        AnswerOption foreignOption = new AnswerOption("чужий варіант", true);

        assertThatThrownBy(() -> new GivenAnswer(question, foreignOption))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
