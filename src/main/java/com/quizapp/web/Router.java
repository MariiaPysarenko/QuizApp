package com.quizapp.web;

/**
 * Єдина точка входу для всіх запитів API (патерн Front Controller).
 * Поки що заглушка — тести RouterTest мають падати.
 */
public class Router {

    public Router add(String method, String pattern, Handler handler) {
        return this;
    }

    public ApiResponse dispatch(String method, String path, BodyReader body, ApiSession session) {
        throw new UnsupportedOperationException("Ще не реалізовано");
    }
}
