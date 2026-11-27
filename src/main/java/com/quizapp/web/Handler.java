package com.quizapp.web;

/**
 * Обробник одного маршруту API.
 */
@FunctionalInterface
public interface Handler {

    ApiResponse handle(ApiRequest request);
}
