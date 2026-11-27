package com.quizapp.web;

import com.quizapp.common.AccessDeniedException;
import com.quizapp.common.ConflictException;
import com.quizapp.common.NotFoundException;
import com.quizapp.common.UnauthorizedException;
import com.quizapp.common.ValidationException;
import com.quizapp.quiz.InvalidQuizException;
import com.quizapp.testsupport.InMemorySession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RouterTest {

    private final Router router = new Router();
    private final InMemorySession session = new InMemorySession();

    private ApiResponse call(String method, String path) {
        return router.dispatch(method, path, BodyReader.EMPTY, session);
    }

    private ApiResponse throwing(RuntimeException exception) {
        router.add("GET", "/api/fail", request -> {
            throw exception;
        });
        return call("GET", "/api/fail");
    }

    @Test
    @DisplayName("Запит потрапляє до обробника з параметрами шляху")
    void dispatchesWithPathParams() {
        router.add("GET", "/api/quizzes/{id}/attempts/{attemptId}",
                request -> ApiResponse.ok(request.longParam("id") + ":" + request.longParam("attemptId")));

        ApiResponse response = call("GET", "/api/quizzes/7/attempts/42");

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("7:42");
    }

    @Test
    @DisplayName("Літеральний сегмент не плутається з параметром")
    void literalSegmentsMustMatch() {
        router.add("POST", "/api/quizzes/{id}/publish", request -> ApiResponse.ok("publish"));
        router.add("POST", "/api/quizzes/import/google-forms", request -> ApiResponse.ok("import"));

        assertThat(call("POST", "/api/quizzes/import/google-forms").body()).isEqualTo("import");
        assertThat(call("POST", "/api/quizzes/5/publish").body()).isEqualTo("publish");
    }

    @Test
    @DisplayName("Скісна риска в кінці шляху не заважає")
    void ignoresTrailingSlash() {
        router.add("GET", "/api/me", request -> ApiResponse.ok("me"));

        assertThat(call("GET", "/api/me/").status()).isEqualTo(200);
    }

    @Test
    @DisplayName("Невідомий шлях — 404")
    void unknownPathGives404() {
        ApiResponse response = call("GET", "/api/unknown");

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.body()).isInstanceOf(ErrorBody.class);
    }

    @Test
    @DisplayName("Відомий шлях з іншим методом — 405")
    void wrongMethodGives405() {
        router.add("GET", "/api/me", request -> ApiResponse.ok("me"));

        assertThat(call("DELETE", "/api/me").status()).isEqualTo(405);
    }

    @Test
    @DisplayName("Нечисловий id у шляху — 400")
    void nonNumericIdGives400() {
        router.add("GET", "/api/quizzes/{id}", request -> ApiResponse.ok(request.longParam("id")));

        assertThat(call("GET", "/api/quizzes/abc").status()).isEqualTo(400);
    }

    @Test
    @DisplayName("Без входу в систему — 401")
    void missingSessionGives401() {
        router.add("GET", "/api/me", request -> ApiResponse.ok(request.requireUserId()));

        assertThat(call("GET", "/api/me").status()).isEqualTo(401);
        session.login(5);
        assertThat(call("GET", "/api/me").body()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Помилки валідації — 400")
    void validationErrorsGive400() {
        assertThat(throwing(new ValidationException("погані дані")).status()).isEqualTo(400);
    }

    @Test
    @DisplayName("Некоректний квіз — 400")
    void invalidQuizGives400() {
        assertThat(throwing(new InvalidQuizException("немає запитань")).status()).isEqualTo(400);
    }

    @Test
    @DisplayName("Невірні облікові дані — 401")
    void unauthorizedGives401() {
        assertThat(throwing(new UnauthorizedException("невірний пароль")).status()).isEqualTo(401);
    }

    @Test
    @DisplayName("Немає прав — 403")
    void accessDeniedGives403() {
        assertThat(throwing(new AccessDeniedException("не ваше")).status()).isEqualTo(403);
    }

    @Test
    @DisplayName("Не знайдено — 404 з повідомленням")
    void notFoundGives404() {
        ApiResponse response = throwing(new NotFoundException("Квіз не знайдено"));

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.body()).isEqualTo(new ErrorBody("Квіз не знайдено"));
    }

    @Test
    @DisplayName("Конфлікт стану — 409")
    void conflictGives409() {
        assertThat(throwing(new ConflictException("вже завершено")).status()).isEqualTo(409);
    }

    @Test
    @DisplayName("Непередбачена помилка — 500 без внутрішніх деталей")
    void unexpectedErrorGives500WithoutDetails() {
        ApiResponse response = throwing(new IllegalStateException("секретна деталь"));

        assertThat(response.status()).isEqualTo(500);
        assertThat(response.body()).isEqualTo(new ErrorBody("Внутрішня помилка сервера"));
    }
}
