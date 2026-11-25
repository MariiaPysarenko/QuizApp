package com.quizapp.attempt.scoring;

import com.quizapp.domain.Question;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.quizapp.attempt.scoring.ScoringTestData.correct;
import static com.quizapp.attempt.scoring.ScoringTestData.question;
import static com.quizapp.attempt.scoring.ScoringTestData.skipped;
import static com.quizapp.attempt.scoring.ScoringTestData.wrong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class WeightedScoringTest {

    private final ScoringStrategy scoring = new WeightedScoring();

    @Test
    @DisplayName("Усі відповіді правильні — максимальний бал і 100%")
    void allCorrectGivesFullScore() {
        ScoringResult result = scoring.score(List.of(
                correct(question(2)),
                correct(question(1))));

        assertThat(result.score()).isEqualTo(3);
        assertThat(result.maxScore()).isEqualTo(3);
        assertThat(result.percent()).isEqualTo(100);
        assertThat(result.mistakes()).isEmpty();
    }

    @Test
    @DisplayName("Неправильна і пропущена відповіді дають 0 балів")
    void wrongAndSkippedGiveZero() {
        ScoringResult result = scoring.score(List.of(
                wrong(question(2)),
                skipped(question(1))));

        assertThat(result.score()).isEqualTo(0);
        assertThat(result.maxScore()).isEqualTo(3);
        assertThat(result.questions())
                .extracting(QuestionResult::points)
                .containsExactly(0.0, 0.0);
    }

    @Test
    @DisplayName("Бали залежать від ваги запитання")
    void usesQuestionPoints() {
        ScoringResult result = scoring.score(List.of(
                correct(question(2)),
                wrong(question(1))));

        assertThat(result.score()).isEqualTo(2);
        assertThat(result.maxScore()).isEqualTo(3);
        assertThat(result.percent()).isCloseTo(66.67, within(0.01));
    }

    @Test
    @DisplayName("Помилки — це неправильні та пропущені запитання")
    void mistakesContainWrongAndSkippedQuestions() {
        Question right = question(1);
        Question wrongOne = question(2);
        Question skippedOne = question(3);

        ScoringResult result = scoring.score(List.of(
                correct(right),
                wrong(wrongOne),
                skipped(skippedOne)));

        assertThat(result.mistakes())
                .extracting(QuestionResult::question)
                .containsExactly(wrongOne, skippedOne);
    }

    @Test
    @DisplayName("Без відповідей — 0 балів і 0% без ділення на нуль")
    void emptyAnswersGiveZeroPercent() {
        ScoringResult result = scoring.score(List.of());

        assertThat(result.score()).isEqualTo(0);
        assertThat(result.maxScore()).isEqualTo(0);
        assertThat(result.percent()).isEqualTo(0);
    }
}
