package com.quizapp.web.http;

import com.quizapp.web.ApiResponse;
import com.quizapp.web.Router;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Єдиний сервлет застосунку: приймає всі запити {@code /api/*} і передає їх у {@link Router}.
 * <p>
 * Тут лише перетворення HTTP ↔ {@code ApiRequest/ApiResponse}; уся логіка — у маршрутизаторі,
 * контролерах і сервісах, які тестуються без сервера.
 */
public class ApiServlet extends HttpServlet {

    private final transient Router router;
    private final transient JsonMapper json;

    public ApiServlet(Router router, JsonMapper json) {
        this.router = router;
        this.json = json;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        ApiResponse result = router.dispatch(request.getMethod(), request.getRequestURI(),
                json.reader(body), new ServletSession(request));

        response.setStatus(result.status());
        if (result.body() != null) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(json.write(result.body()));
        }
    }
}
