package com.quizapp.attempt.scoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.quizapp.attempt.scoring.ScoringTestData.correct;
import static com.quizapp.attempt.scoring.ScoringTestData.question;
import static com.quizapp.attempt.scoring.ScoringTestData.skipped;
import static com.quizapp.attempt.scoring.ScoringTestData.wrong;
import static org.assertj.core.api.Assertions.assertThat;

class PenaltyScoringTest {

    private final ScoringStrategy scoring = new PenaltyScoring();

    @Test
    @DisplayName("Правильна відповідь дає повні бали")
    void correctAnswerGivesFullPoints() {
        ScoringResult result = scoring.score(List.of(correct(question(4))));

        assertThat(result.score()).isEqualTo(4);
    }

    @Test
    @DisplayName("Неправильна відповідь знімає чверть балів запитання")
    void wrongAnswerSubtractsQuarterOfPoints() {
        ScoringResult result = scoring.score(List.of(
                correct(question(4)),
                wrong(question(2))));

        assertThat(result.questions())
                .extracting(QuestionResult::points)
                .containsExactly(4.0, -0.5);
        assertThat(result.score()).isEqualTo(3.5);
    }

    @Test
    @DisplayName("Пропущене запитання не штрафується")
    void skippedAnswerIsNotPenalized() {
        ScoringResult result = scoring.score(List.of(
                correct(question(4)),
                skipped(question(2))));

        assertThat(result.score()).isEqualTo(4);
    }

    @Test
    @DisplayName("Підсумок не буває від'ємним")
    void totalIsNeverNegative() {
        ScoringResult result = scoring.score(List.of(
                wrong(question(4)),
                wrong(question(2))));

        assertThat(result.score()).isEqualTo(0);
        assertThat(result.maxScore()).isEqualTo(6);
    }

    @Test
    @DisplayName("Змішані відповіді: правильна, неправильна, пропущена")
    void mixedAnswers() {
        ScoringResult result = scoring.score(List.of(
                correct(question(4)),
                wrong(question(2)),
                skipped(question(1))));

        assertThat(result.score()).isEqualTo(3.5);
        assertThat(result.maxScore()).isEqualTo(7);
        assertThat(result.percent()).isEqualTo(50);
        assertThat(result.mistakes()).hasSize(2);
    }
}
