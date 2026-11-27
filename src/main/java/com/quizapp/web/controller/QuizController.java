package com.quizapp.web.controller;

import com.quizapp.domain.Quiz;
import com.quizapp.domain.Role;
import com.quizapp.domain.User;
import com.quizapp.importing.ImportResult;
import com.quizapp.importing.QuizImportService;
import com.quizapp.importing.googleforms.GoogleFormExport;
import com.quizapp.importing.googleforms.GoogleFormsQuizAdapter;
import com.quizapp.quiz.QuizService;
import com.quizapp.web.ApiRequest;
import com.quizapp.web.ApiResponse;
import com.quizapp.web.CurrentUser;
import com.quizapp.web.Router;
import com.quizapp.web.dto.CreateQuizRequest;
import com.quizapp.web.dto.ImportView;
import com.quizapp.web.dto.QuizSummaryView;
import com.quizapp.web.dto.QuizView;

/**
 * Квізи: список, перегляд, створення, публікація, імпорт із Google Forms.
 */
public class QuizController {

    private final QuizService quizzes;
    private final QuizImportService importer;
    private final CurrentUser currentUser;

    public QuizController(QuizService quizzes, QuizImportService importer, CurrentUser currentUser) {
        this.quizzes = quizzes;
        this.importer = importer;
        this.currentUser = currentUser;
    }

    public void routes(Router router) {
        router.add("GET", "/api/quizzes", this::list);
        router.add("POST", "/api/quizzes", this::create);
        router.add("POST", "/api/quizzes/import/google-forms", this::importGoogleForm);
        router.add("GET", "/api/quizzes/{id}", this::get);
        router.add("POST", "/api/quizzes/{id}/publish", this::publish);
    }

    private ApiResponse list(ApiRequest request) {
        User user = currentUser.require(request);
        return ApiResponse.ok(quizzes.list(user).stream()
                .map(QuizSummaryView::from)
                .toList());
    }

    private ApiResponse get(ApiRequest request) {
        User user = currentUser.require(request);
        Quiz quiz = quizzes.get(user, request.longParam("id"));
        QuizView view = quiz.isCreatedBy(user.id()) ? QuizView.forAuthor(quiz) : QuizView.forStudent(quiz);
        return ApiResponse.ok(view);
    }

    private ApiResponse create(ApiRequest request) {
        User teacher = currentUser.requireRole(request, Role.TEACHER);
        CreateQuizRequest body = request.body(CreateQuizRequest.class);
        Quiz quiz = quizzes.create(teacher.id(), body.toBuilder());
        return ApiResponse.created(QuizView.forAuthor(quiz));
    }

    private ApiResponse publish(ApiRequest request) {
        User teacher = currentUser.requireRole(request, Role.TEACHER);
        Quiz quiz = quizzes.publish(teacher.id(), request.longParam("id"));
        return ApiResponse.ok(QuizSummaryView.from(quiz));
    }

    private ApiResponse importGoogleForm(ApiRequest request) {
        User teacher = currentUser.requireRole(request, Role.TEACHER);
        GoogleFormExport export = request.body(GoogleFormExport.class);
        ImportResult result = importer.importQuiz(new GoogleFormsQuizAdapter(export), teacher.id());
        return ApiResponse.created(ImportView.from(result));
    }
}
