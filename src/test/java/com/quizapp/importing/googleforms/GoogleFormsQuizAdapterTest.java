package com.quizapp.importing.googleforms;

import com.quizapp.domain.AnswerOption;
import com.quizapp.domain.Question;
import com.quizapp.domain.Quiz;
import com.quizapp.domain.QuizStatus;
import com.quizapp.importing.ImportResult;
import com.quizapp.importing.SkippedItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.quizapp.importing.googleforms.GoogleFormsTestData.form;
import static com.quizapp.importing.googleforms.GoogleFormsTestData.item;
import static com.quizapp.importing.googleforms.GoogleFormsTestData.multipleChoice;
import static com.quizapp.importing.googleforms.GoogleFormsTestData.right;
import static com.quizapp.importing.googleforms.GoogleFormsTestData.wrong;
import static org.assertj.core.api.Assertions.assertThat;

class GoogleFormsQuizAdapterTest {

    private static final long TEACHER = 10;

    private static ImportResult adapt(GoogleFormExport export) {
        return new GoogleFormsQuizAdapter(export).toQuiz(TEACHER);
    }

    @Test
    @DisplayName("Запитання з одним варіантом відповіді переносяться в квіз-чернетку викладача")
    void importsMultipleChoiceQuestions() {
        ImportResult result = adapt(form("Основи Java",
                multipleChoice("Що виконує байт-код?", right("JVM"), wrong("javac")),
                multipleChoice("Який модифікатор приховує поле?", wrong("public"), right("private"))));

        Quiz quiz = result.quiz();
        assertThat(quiz.title()).isEqualTo("Основи Java");
        assertThat(quiz.description()).isEqualTo("Опис форми");
        assertThat(quiz.status()).isEqualTo(QuizStatus.DRAFT);
        assertThat(quiz.creatorId()).isEqualTo(TEACHER);
        assertThat(quiz.questions())
                .extracting(Question::text)
                .containsExactly("Що виконує байт-код?", "Який модифікатор приховує поле?");
        assertThat(quiz.questions().get(1).options())
                .extracting(AnswerOption::text)
                .containsExactly("public", "private");
        assertThat(quiz.questions().get(1).options())
                .filteredOn(AnswerOption::correct)
                .extracting(AnswerOption::text)
                .containsExactly("private");
        assertThat(result.importedCount()).isEqualTo(2);
        assertThat(result.skipped()).isEmpty();
    }

    @Test
    @DisplayName("Випадний список (LIST) імпортується так само, як радіокнопки")
    void importsDropdownQuestions() {
        ImportResult result = adapt(form("Квіз",
                item("LIST", "Столиця України?", right("Київ"), wrong("Львів"))));

        assertThat(result.importedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Бали та пояснення до неправильної відповіді переносяться")
    void importsPointsAndFeedback() {
        GoogleFormExport.Item question = new GoogleFormExport.Item("MULTIPLE_CHOICE", "2 + 2 = ?", 3,
                "Перевірте ще раз таблицю додавання", List.of(right("4"), wrong("5")));

        Question imported = adapt(form("Квіз", question)).quiz().questions().get(0);

        assertThat(imported.points()).isEqualTo(3);
        assertThat(imported.explanation()).isEqualTo("Перевірте ще раз таблицю додавання");
    }

    @Test
    @DisplayName("Якщо бали не задані або 0, запитання коштує 1 бал")
    void defaultsPointsToOne() {
        GoogleFormExport.Item noPoints = new GoogleFormExport.Item("MULTIPLE_CHOICE", "A?", null, null,
                List.of(right("так"), wrong("ні")));
        GoogleFormExport.Item zeroPoints = new GoogleFormExport.Item("MULTIPLE_CHOICE", "B?", 0, null,
                List.of(right("так"), wrong("ні")));

        Quiz quiz = adapt(form("Квіз", noPoints, zeroPoints)).quiz();

        assertThat(quiz.questions())
                .extracting(Question::points)
                .containsExactly(1, 1);
    }

    @Test
    @DisplayName("Запитання з прапорцями (кілька відповідей) пропускаються з причиною")
    void skipsCheckboxQuestions() {
        ImportResult result = adapt(form("Квіз",
                multipleChoice("A?", right("так"), wrong("ні")),
                item("CHECKBOX", "Оберіть парні", right("2"), right("4"), wrong("3"))));

        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.skipped()).hasSize(1);
        SkippedItem skipped = result.skipped().get(0);
        assertThat(skipped.position()).isEqualTo(2);
        assertThat(skipped.title()).isEqualTo("Оберіть парні");
        assertThat(skipped.reason()).contains("CHECKBOX");
    }

    @Test
    @DisplayName("Текстові запитання пропускаються")
    void skipsTextQuestions() {
        ImportResult result = adapt(form("Квіз",
                multipleChoice("A?", right("так"), wrong("ні")),
                item("TEXT", "Ваше ім'я"),
                item("PARAGRAPH_TEXT", "Поясніть відповідь")));

        assertThat(result.skipped())
                .extracting(SkippedItem::title)
                .containsExactly("Ваше ім'я", "Поясніть відповідь");
    }

    @Test
    @DisplayName("Запитання без позначеної правильної відповіді пропускається")
    void skipsQuestionWithoutCorrectAnswer() {
        ImportResult result = adapt(form("Квіз",
                multipleChoice("A?", right("так"), wrong("ні")),
                multipleChoice("B?", wrong("так"), wrong("ні"))));

        assertThat(result.skipped()).hasSize(1);
        assertThat(result.skipped().get(0).reason()).contains("правильну відповідь");
    }

    @Test
    @DisplayName("Запитання з кількома правильними відповідями пропускається")
    void skipsQuestionWithSeveralCorrectAnswers() {
        ImportResult result = adapt(form("Квіз",
                multipleChoice("A?", right("так"), wrong("ні")),
                multipleChoice("B?", right("так"), right("ні"))));

        assertThat(result.skipped().get(0).reason()).contains("кілька правильних");
    }

    @Test
    @DisplayName("Запитання з одним варіантом пропускається")
    void skipsQuestionWithOneChoice() {
        ImportResult result = adapt(form("Квіз",
                multipleChoice("A?", right("так"), wrong("ні")),
                multipleChoice("B?", right("так"))));

        assertThat(result.skipped().get(0).reason()).contains("менше двох");
    }

    @Test
    @DisplayName("Заголовки секцій, розриви сторінок і зображення ігноруються без звіту")
    void ignoresLayoutItems() {
        ImportResult result = adapt(form("Квіз",
                item("SECTION_HEADER", "Частина 1"),
                multipleChoice("A?", right("так"), wrong("ні")),
                item("PAGE_BREAK", "Сторінка 2"),
                item("IMAGE", "Схема")));

        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.skipped()).isEmpty();
    }

    @Test
    @DisplayName("Якщо жодне запитання не підходить — квізу немає, але є звіт")
    void noQuizWhenNothingImported() {
        ImportResult result = adapt(form("Опитування",
                item("TEXT", "Ваше ім'я"),
                item("CHECKBOX", "Хобі", right("спорт"), right("музика"))));

        assertThat(result.hasQuiz()).isFalse();
        assertThat(result.importedCount()).isEqualTo(0);
        assertThat(result.skipped()).hasSize(2);
    }

    @Test
    @DisplayName("Форма без назви отримує назву за замовчуванням")
    void usesDefaultTitleForUntitledForm() {
        ImportResult result = adapt(form("  ",
                multipleChoice("A?", right("так"), wrong("ні"))));

        assertThat(result.quiz().title()).isEqualTo("Квіз з Google Forms");
    }
}
