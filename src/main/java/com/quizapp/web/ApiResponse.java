package com.quizapp.web;

/**
 * Відповідь API: HTTP-статус і тіло, яке буде перетворено на JSON.
 */
public record ApiResponse(int status, Object body) {

    public static ApiResponse ok(Object body) {
        return new ApiResponse(200, body);
    }

    public static ApiResponse created(Object body) {
        return new ApiResponse(201, body);
    }

    public static ApiResponse noContent() {
        return new ApiResponse(204, null);
    }

    public static ApiResponse error(int status, String message) {
        return new ApiResponse(status, new ErrorBody(message));
    }
}
