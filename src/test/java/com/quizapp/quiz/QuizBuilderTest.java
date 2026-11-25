package com.quizapp.quiz;

import com.quizapp.domain.AnswerOption;
import com.quizapp.domain.Question;
import com.quizapp.domain.Quiz;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuizBuilderTest {

    @Test
    @DisplayName("Збирає квіз із запитаннями, балами, поясненням і варіантами")
    void buildsQuizWithQuestionsAndOptions() {
        Quiz quiz = new QuizBuilder()
                .title("Основи Java")
                .description("ООП")
                .question("Який модифікатор робить поле видимим лише в класі?")
                .wrong("public")
                .correct("private")
                .points(2)
                .explanation("Див. тему інкапсуляції")
                .question("Що виконує байт-код?")
                .correct("JVM")
                .wrong("javac")
                .build();

        assertThat(quiz.title()).isEqualTo("Основи Java");
        assertThat(quiz.description()).isEqualTo("ООП");
        assertThat(quiz.questions()).hasSize(2);

        Question first = quiz.questions().get(0);
        assertThat(first.text()).isEqualTo("Який модифікатор робить поле видимим лише в класі?");
        assertThat(first.points()).isEqualTo(2);
        assertThat(first.explanation()).isEqualTo("Див. тему інкапсуляції");
        assertThat(first.options())
                .extracting(AnswerOption::text)
                .containsExactly("public", "private");
        assertThat(first.options())
                .filteredOn(AnswerOption::correct)
                .extracting(AnswerOption::text)
                .containsExactly("private");
    }

    @Test
    @DisplayName("Запитання за замовчуванням коштує 1 бал")
    void questionHasOnePointByDefault() {
        Quiz quiz = new QuizBuilder()
                .title("Квіз")
                .question("2 + 2 = ?").correct("4").wrong("5")
                .build();

        assertThat(quiz.questions().get(0).points()).isEqualTo(1);
    }

    @Test
    @DisplayName("Не можна створити квіз без назви")
    void rejectsQuizWithoutTitle() {
        QuizBuilder builder = new QuizBuilder()
                .title("   ")
                .question("2 + 2 = ?").correct("4").wrong("5");

        assertThatThrownBy(builder::build)
                .isInstanceOf(InvalidQuizException.class)
                .hasMessageContaining("назву");
    }

    @Test
    @DisplayName("Не можна створити квіз без запитань")
    void rejectsQuizWithoutQuestions() {
        QuizBuilder builder = new QuizBuilder().title("Порожній квіз");

        assertThatThrownBy(builder::build)
                .isInstanceOf(InvalidQuizException.class)
                .hasMessageContaining("хоча б одне запитання");
    }

    @Test
    @DisplayName("Запитання без правильної відповіді відхиляється")
    void rejectsQuestionWithoutCorrectOption() {
        QuizBuilder builder = new QuizBuilder()
                .title("Квіз")
                .question("2 + 2 = ?").wrong("3").wrong("5");

        assertThatThrownBy(builder::build)
                .isInstanceOf(InvalidQuizException.class)
                .hasMessageContaining("Запитання 1")
                .hasMessageContaining("рівно одна правильна");
    }

    @Test
    @DisplayName("Запитання з кількома правильними відповідями відхиляється")
    void rejectsQuestionWithSeveralCorrectOptions() {
        QuizBuilder builder = new QuizBuilder()
                .title("Квіз")
                .question("2 + 2 = ?").correct("4").wrong("5")
                .question("Парні числа?").correct("2").correct("4").wrong("3");

        assertThatThrownBy(builder::build)
                .isInstanceOf(InvalidQuizException.class)
                .hasMessageContaining("Запитання 2")
                .hasMessageContaining("рівно одна правильна");
    }

    @Test
    @DisplayName("Запитання має містити щонайменше два варіанти")
    void rejectsQuestionWithLessThanTwoOptions() {
        QuizBuilder builder = new QuizBuilder()
                .title("Квіз")
                .question("2 + 2 = ?").correct("4");

        assertThatThrownBy(builder::build)
                .isInstanceOf(InvalidQuizException.class)
                .hasMessageContaining("щонайменше 2 варіанти");
    }

    @Test
    @DisplayName("Кількість балів має бути додатною")
    void rejectsNonPositivePoints() {
        QuizBuilder builder = new QuizBuilder()
                .title("Квіз")
                .question("2 + 2 = ?").correct("4").wrong("5").points(0);

        assertThatThrownBy(builder::build)
                .isInstanceOf(InvalidQuizException.class)
                .hasMessageContaining("балів");
    }

    @Test
    @DisplayName("Варіант відповіді не можна додати до першого запитання")
    void rejectsOptionBeforeQuestion() {
        QuizBuilder builder = new QuizBuilder().title("Квіз");

        assertThatThrownBy(() -> builder.correct("4"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Зібраний квіз не можна змінити ззовні")
    void builtQuizIsImmutable() {
        Quiz quiz = new QuizBuilder()
                .title("Квіз")
                .question("2 + 2 = ?").correct("4").wrong("5")
                .build();

        assertThatThrownBy(() -> quiz.questions().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> quiz.questions().get(0).options().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
