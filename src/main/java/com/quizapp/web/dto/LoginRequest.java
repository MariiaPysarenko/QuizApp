package com.quizapp.web.dto;

/** Тіло запиту POST /api/auth/login. */
public record LoginRequest(String email, String password) {
}
