package com.quizapp.web.controller;

import com.quizapp.auth.AuthService;
import com.quizapp.domain.User;
import com.quizapp.web.ApiRequest;
import com.quizapp.web.ApiResponse;
import com.quizapp.web.CurrentUser;
import com.quizapp.web.Router;
import com.quizapp.web.dto.LoginRequest;
import com.quizapp.web.dto.RegisterRequest;
import com.quizapp.web.dto.UserView;

/**
 * Реєстрація, вхід, вихід і поточний користувач.
 */
public class AuthController {

    private final AuthService auth;
    private final CurrentUser currentUser;

    public AuthController(AuthService auth, CurrentUser currentUser) {
        this.auth = auth;
        this.currentUser = currentUser;
    }

    public void routes(Router router) {
        router.add("POST", "/api/auth/register", this::register);
        router.add("POST", "/api/auth/login", this::login);
        router.add("POST", "/api/auth/logout", this::logout);
        router.add("GET", "/api/me", this::me);
    }

    private ApiResponse register(ApiRequest request) {
        RegisterRequest body = request.body(RegisterRequest.class);
        User user = auth.register(body.email(), body.password(), body.role(), body.inviteCode());
        return ApiResponse.created(UserView.from(user));
    }

    private ApiResponse login(ApiRequest request) {
        LoginRequest body = request.body(LoginRequest.class);
        User user = auth.login(body.email(), body.password());
        request.session().login(user.id());
        return ApiResponse.ok(UserView.from(user));
    }

    private ApiResponse logout(ApiRequest request) {
        request.session().logout();
        return ApiResponse.noContent();
    }

    private ApiResponse me(ApiRequest request) {
        return ApiResponse.ok(UserView.from(currentUser.require(request)));
    }
}
