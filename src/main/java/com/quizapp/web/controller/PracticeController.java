package com.quizapp.web.controller;

import com.quizapp.attempt.scoring.ScoringResult;
import com.quizapp.domain.Role;
import com.quizapp.domain.User;
import com.quizapp.practice.PracticeService;
import com.quizapp.web.ApiRequest;
import com.quizapp.web.ApiResponse;
import com.quizapp.web.CurrentUser;
import com.quizapp.web.Router;
import com.quizapp.web.dto.PracticeView;
import com.quizapp.web.dto.QuizView;
import com.quizapp.web.dto.ResultView;
import com.quizapp.web.dto.SubmitAnswersRequest;

/**
 * Робота над помилками.
 */
public class PracticeController {

    private final PracticeService practice;
    private final CurrentUser currentUser;

    public PracticeController(PracticeService practice, CurrentUser currentUser) {
        this.practice = practice;
        this.currentUser = currentUser;
    }

    public void routes(Router router) {
        router.add("GET", "/api/me/practice", this::get);
        router.add("POST", "/api/me/practice/submit", this::submit);
    }

    private ApiResponse get(ApiRequest request) {
        User student = currentUser.requireRole(request, Role.STUDENT);
        return ApiResponse.ok(new PracticeView(practice.buildPractice(student.id()).stream()
                .map(question -> QuizView.question(question, false))
                .toList()));
    }

    private ApiResponse submit(ApiRequest request) {
        User student = currentUser.requireRole(request, Role.STUDENT);
        SubmitAnswersRequest body = request.body(SubmitAnswersRequest.class);
        ScoringResult result = practice.submit(student.id(), body.toMap());
        return ApiResponse.ok(ResultView.from(result));
    }
}
