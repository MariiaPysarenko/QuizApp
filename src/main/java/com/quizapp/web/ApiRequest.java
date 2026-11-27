package com.quizapp.web;

import com.quizapp.common.UnauthorizedException;
import com.quizapp.common.ValidationException;

import java.util.Map;

/**
 * Запит до API, незалежний від сервлетів.
 *
 * @param pathParams значення з шаблону шляху, наприклад {@code id} для {@code /api/quizzes/{id}}
 */
public record ApiRequest(String method,
                         String path,
                         Map<String, String> pathParams,
                         BodyReader body,
                         ApiSession session) {

    public ApiRequest {
        pathParams = Map.copyOf(pathParams);
    }

    public long longParam(String name) {
        String value = pathParams.get(name);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new ValidationException("Параметр " + name + " має бути числом: " + value);
        }
    }

    public <T> T body(Class<T> type) {
        T value = body.read(type);
        if (value == null) {
            throw new ValidationException("Тіло запиту відсутнє");
        }
        return value;
    }

    public long requireUserId() {
        return session.userId()
                .orElseThrow(() -> new UnauthorizedException("Потрібно увійти в систему"));
    }
}
