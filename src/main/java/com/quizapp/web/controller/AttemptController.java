package com.quizapp.web.controller;

import com.quizapp.attempt.AttemptService;
import com.quizapp.attempt.scoring.ScoringResult;
import com.quizapp.domain.Attempt;
import com.quizapp.domain.AttemptType;
import com.quizapp.domain.Quiz;
import com.quizapp.domain.Role;
import com.quizapp.domain.User;
import com.quizapp.quiz.QuizService;
import com.quizapp.web.ApiRequest;
import com.quizapp.web.ApiResponse;
import com.quizapp.web.CurrentUser;
import com.quizapp.web.Router;
import com.quizapp.web.dto.AttemptSummaryView;
import com.quizapp.web.dto.QuizView;
import com.quizapp.web.dto.ResultView;
import com.quizapp.web.dto.StartAttemptView;
import com.quizapp.web.dto.SubmitAnswersRequest;

import java.time.Instant;

/**
 * Проходження квізу та історія спроб.
 */
public class AttemptController {

    static final String PRACTICE_TITLE = "Робота над помилками";

    private final AttemptService attempts;
    private final QuizService quizzes;
    private final CurrentUser currentUser;

    public AttemptController(AttemptService attempts, QuizService quizzes, CurrentUser currentUser) {
        this.attempts = attempts;
        this.quizzes = quizzes;
        this.currentUser = currentUser;
    }

    public void routes(Router router) {
        router.add("POST", "/api/quizzes/{id}/attempts", this::start);
        router.add("POST", "/api/attempts/{id}/submit", this::submit);
        router.add("GET", "/api/me/attempts", this::history);
    }

    private ApiResponse start(ApiRequest request) {
        User student = currentUser.requireRole(request, Role.STUDENT);
        long quizId = request.longParam("id");
        Attempt attempt = attempts.start(student.id(), quizId);
        Quiz quiz = quizzes.get(student, quizId);
        Instant deadline = quiz.timeLimit() == null ? null : attempt.startedAt().plus(quiz.timeLimit());
        return ApiResponse.created(new StartAttemptView(attempt.id(), attempt.startedAt(), deadline,
                QuizView.forStudent(quiz)));
    }

    private ApiResponse submit(ApiRequest request) {
        User student = currentUser.requireRole(request, Role.STUDENT);
        SubmitAnswersRequest body = request.body(SubmitAnswersRequest.class);
        ScoringResult result = attempts.submit(student.id(), request.longParam("id"), body.toMap());
        return ApiResponse.ok(ResultView.from(result));
    }

    private ApiResponse history(ApiRequest request) {
        User student = currentUser.requireRole(request, Role.STUDENT);
        return ApiResponse.ok(attempts.history(student.id()).stream()
                .map(attempt -> AttemptSummaryView.from(attempt, titleOf(attempt)))
                .toList());
    }

    private String titleOf(Attempt attempt) {
        if (attempt.type() == AttemptType.PRACTICE) {
            return PRACTICE_TITLE;
        }
        return quizzes.find(attempt.quizId()).map(Quiz::title).orElse(null);
    }
}
