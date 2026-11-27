package com.quizapp.web;

import com.quizapp.auth.UserRepository;
import com.quizapp.common.AccessDeniedException;
import com.quizapp.common.UnauthorizedException;
import com.quizapp.domain.Role;
import com.quizapp.domain.User;

/**
 * Визначає поточного користувача за сесією та перевіряє його роль.
 */
public class CurrentUser {

    private final UserRepository users;

    public CurrentUser(UserRepository users) {
        this.users = users;
    }

    public User require(ApiRequest request) {
        long userId = request.requireUserId();
        return users.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Сесія недійсна, увійдіть знову"));
    }

    public User requireRole(ApiRequest request, Role role) {
        User user = require(request);
        if (user.role() != role) {
            throw new AccessDeniedException("Дія доступна лише для ролі " + role);
        }
        return user;
    }
}
