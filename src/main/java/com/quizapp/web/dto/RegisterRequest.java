package com.quizapp.web.dto;

import com.quizapp.domain.Role;

/** Тіло запиту POST /api/auth/register. */
public record RegisterRequest(String email, String password, Role role, String inviteCode) {
}
