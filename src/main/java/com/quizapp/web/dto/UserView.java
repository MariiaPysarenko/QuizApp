package com.quizapp.web.dto;

import com.quizapp.domain.Role;
import com.quizapp.domain.User;

/** Користувач у відповіді API — без хешу пароля. */
public record UserView(long id, String email, Role role) {

    public static UserView from(User user) {
        return new UserView(user.id(), user.email(), user.role());
    }
}
