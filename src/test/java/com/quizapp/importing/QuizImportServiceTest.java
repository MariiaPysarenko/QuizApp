package com.quizapp.importing;

import com.quizapp.common.ValidationException;
import com.quizapp.domain.Quiz;
import com.quizapp.domain.QuizStatus;
import com.quizapp.importing.googleforms.GoogleFormExport;
import com.quizapp.importing.googleforms.GoogleFormsQuizAdapter;
import com.quizapp.persistence.memory.InMemoryQuizRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuizImportServiceTest {

    private static final long TEACHER = 10;

    private final InMemoryQuizRepository quizzes = new InMemoryQuizRepository();
    private final QuizImportService service = new QuizImportService(quizzes);

    @Test
    @DisplayName("Імпортований квіз зберігається як чернетка викладача")
    void savesImportedQuiz() {
        ImportResult result = service.importQuiz(googleForm(
                question("MULTIPLE_CHOICE", "A?", true, false),
                question("CHECKBOX", "B?", true, true)), TEACHER);

        Quiz saved = result.quiz();
        assertThat(saved.id()).isNotNull();
        assertThat(saved.questions().get(0).id()).isNotNull();
        assertThat(quizzes.findById(saved.id()).orElseThrow().status()).isEqualTo(QuizStatus.DRAFT);
        assertThat(saved.creatorId()).isEqualTo(TEACHER);
        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.skipped()).hasSize(1);
    }

    @Test
    @DisplayName("Якщо не імпортовано жодного запитання — помилка з причинами, квіз не зберігається")
    void rejectsImportWithoutQuestions() {
        assertThatThrownBy(() -> service.importQuiz(googleForm(
                question("CHECKBOX", "Хобі", true, true)), TEACHER))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("жодного запитання")
                .hasMessageContaining("Хобі");
        assertThat(quizzes.findById(1).isPresent()).isFalse();
    }

    private static GoogleFormsQuizAdapter googleForm(GoogleFormExport.Item... items) {
        return new GoogleFormsQuizAdapter(
                new GoogleFormExport("google-forms", "form-1", "Форма", null, true, List.of(items)));
    }

    private static GoogleFormExport.Item question(String type, String title, boolean first, boolean second) {
        return new GoogleFormExport.Item(type, title, 1, null, List.of(
                new GoogleFormExport.Choice("перший", first),
                new GoogleFormExport.Choice("другий", second)));
    }
}
