package com.quizapp.web;

import com.quizapp.common.AccessDeniedException;
import com.quizapp.common.ConflictException;
import com.quizapp.common.NotFoundException;
import com.quizapp.common.UnauthorizedException;
import com.quizapp.common.ValidationException;
import com.quizapp.quiz.InvalidQuizException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Єдина точка входу для всіх запитів API (патерн Front Controller).
 * <p>
 * Знаходить обробник за методом і шаблоном шляху, а винятки сервісів перетворює
 * на HTTP-статуси. Завдяки цьому контролери й сервіси не працюють з HTTP-кодами напряму.
 */
public class Router {

    private static final Logger LOG = Logger.getLogger(Router.class.getName());

    private final List<Route> routes = new ArrayList<>();

    public Router add(String method, String pattern, Handler handler) {
        routes.add(new Route(method, split(pattern), handler));
        return this;
    }

    public ApiResponse dispatch(String method, String path, BodyReader body, ApiSession session) {
        String[] segments = split(path);
        boolean pathMatched = false;
        for (Route route : routes) {
            Map<String, String> params = route.match(segments);
            if (params == null) {
                continue;
            }
            pathMatched = true;
            if (route.method().equalsIgnoreCase(method)) {
                return handle(route.handler(), new ApiRequest(method, path, params, body, session));
            }
        }
        return pathMatched
                ? ApiResponse.error(405, "Метод " + method + " не підтримується для " + path)
                : ApiResponse.error(404, "Маршрут не знайдено: " + path);
    }

    private static ApiResponse handle(Handler handler, ApiRequest request) {
        try {
            return handler.handle(request);
        } catch (ValidationException | InvalidQuizException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (UnauthorizedException e) {
            return ApiResponse.error(401, e.getMessage());
        } catch (AccessDeniedException e) {
            return ApiResponse.error(403, e.getMessage());
        } catch (NotFoundException e) {
            return ApiResponse.error(404, e.getMessage());
        } catch (ConflictException e) {
            return ApiResponse.error(409, e.getMessage());
        } catch (RuntimeException e) {
            // Деталі — лише в журнал сервера: клієнту не варто бачити внутрішню будову системи.
            LOG.log(Level.SEVERE, "Помилка під час обробки " + request.method() + " " + request.path(), e);
            return ApiResponse.error(500, "Внутрішня помилка сервера");
        }
    }

    private static String[] split(String path) {
        String trimmed = path == null ? "" : path.strip();
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? new String[0] : trimmed.split("/");
    }

    private record Route(String method, String[] pattern, Handler handler) {

        /** Параметри шляху, якщо шлях підходить під шаблон, інакше {@code null}. */
        Map<String, String> match(String[] segments) {
            if (segments.length != pattern.length) {
                return null;
            }
            Map<String, String> params = new HashMap<>();
            for (int i = 0; i < pattern.length; i++) {
                String expected = pattern[i];
                if (expected.startsWith("{") && expected.endsWith("}")) {
                    params.put(expected.substring(1, expected.length() - 1), segments[i]);
                } else if (!expected.equals(segments[i])) {
                    return null;
                }
            }
            return params;
        }
    }
}
