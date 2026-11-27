package com.quizapp.quiz;

import com.quizapp.common.AccessDeniedException;
import com.quizapp.common.ConflictException;
import com.quizapp.common.NotFoundException;
import com.quizapp.domain.Quiz;
import com.quizapp.domain.QuizStatus;
import com.quizapp.domain.Role;
import com.quizapp.domain.User;
import com.quizapp.persistence.memory.InMemoryQuizRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuizServiceTest {

    private static final User TEACHER = user(1, Role.TEACHER);
    private static final User OTHER_TEACHER = user(2, Role.TEACHER);
    private static final User STUDENT = user(3, Role.STUDENT);

    private final InMemoryQuizRepository quizzes = new InMemoryQuizRepository();
    private final QuizService service = new QuizService(quizzes);

    @Test
    @DisplayName("Створений квіз — чернетка викладача, навіть якщо клієнт вказав інше")
    void createsDraftOfTeacher() {
        Quiz quiz = service.create(TEACHER.id(), quiz("Квіз").status(QuizStatus.PUBLISHED).creatorId(99));

        assertThat(quiz.id()).isNotNull();
        assertThat(quiz.creatorId()).isEqualTo(TEACHER.id());
        assertThat(quiz.status()).isEqualTo(QuizStatus.DRAFT);
    }

    @Test
    @DisplayName("Студент бачить лише опубліковані квізи")
    void studentSeesOnlyPublished() {
        Quiz published = publishedQuiz(TEACHER, "Опублікований");
        service.create(TEACHER.id(), quiz("Чернетка"));

        assertThat(service.list(STUDENT))
                .extracting(Quiz::id)
                .containsExactly(published.id());
    }

    @Test
    @DisplayName("Викладач бачить опубліковані квізи та власні чернетки, але не чужі")
    void teacherSeesPublishedAndOwnDrafts() {
        Quiz published = publishedQuiz(OTHER_TEACHER, "Чужий опублікований");
        Quiz ownDraft = service.create(TEACHER.id(), quiz("Моя чернетка"));
        service.create(OTHER_TEACHER.id(), quiz("Чужа чернетка"));

        assertThat(service.list(TEACHER))
                .extracting(Quiz::id)
                .containsExactly(published.id(), ownDraft.id());
    }

    @Test
    @DisplayName("Чернетку бачить лише автор; для інших її не існує")
    void draftIsVisibleOnlyToAuthor() {
        Quiz draft = service.create(TEACHER.id(), quiz("Чернетка"));

        assertThat(service.get(TEACHER, draft.id()).id()).isEqualTo(draft.id());
        assertThatThrownBy(() -> service.get(STUDENT, draft.id()))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.get(OTHER_TEACHER, draft.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Автор публікує квіз, і студенти його бачать")
    void authorPublishesQuiz() {
        Quiz draft = service.create(TEACHER.id(), quiz("Квіз"));

        Quiz published = service.publish(TEACHER.id(), draft.id());

        assertThat(published.status()).isEqualTo(QuizStatus.PUBLISHED);
        assertThat(service.get(STUDENT, draft.id()).id()).isEqualTo(draft.id());
    }

    @Test
    @DisplayName("Чужий квіз опублікувати не можна")
    void cannotPublishSomeoneElsesQuiz() {
        Quiz draft = service.create(TEACHER.id(), quiz("Квіз"));

        assertThatThrownBy(() -> service.publish(OTHER_TEACHER.id(), draft.id()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Неіснуючий квіз опублікувати не можна")
    void cannotPublishMissingQuiz() {
        assertThatThrownBy(() -> service.publish(TEACHER.id(), 999))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Архівний квіз опублікувати не можна")
    void cannotPublishArchivedQuiz() {
        Quiz archived = quizzes.save(quiz("Архів").creatorId(TEACHER.id()).status(QuizStatus.ARCHIVED).build());

        assertThatThrownBy(() -> service.publish(TEACHER.id(), archived.id()))
                .isInstanceOf(ConflictException.class);
    }

    private Quiz publishedQuiz(User author, String title) {
        Quiz draft = service.create(author.id(), quiz(title));
        return service.publish(author.id(), draft.id());
    }

    private static QuizBuilder quiz(String title) {
        return new QuizBuilder()
                .title(title)
                .question("2 + 2 = ?").correct("4").wrong("5");
    }

    private static User user(long id, Role role) {
        return new User(id, "user" + id + "@example.com", "hash", role, Instant.EPOCH);
    }
}
