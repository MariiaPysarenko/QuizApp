package com.quizapp;

import com.quizapp.auth.Pbkdf2PasswordHasher;
import com.quizapp.web.QuizApp;
import com.quizapp.web.http.ApiServlet;
import com.quizapp.web.http.JsonMapper;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;

/**
 * Запуск Quiz App: вбудований Tomcat з одним сервлетом для {@code /api/*}.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.fromEnvironment();
        QuizApp app = QuizApp.inMemory(new Pbkdf2PasswordHasher(), config.teacherInviteCode(), Clock.systemUTC());

        Path workDir = Files.createTempDirectory("quizapp-tomcat");
        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(workDir.toString());
        tomcat.setPort(config.port());
        tomcat.getConnector();

        Context context = tomcat.addContext("", workDir.toAbsolutePath().toString());
        Tomcat.addServlet(context, "api", new ApiServlet(app.router(), new JsonMapper()));
        context.addServletMappingDecoded("/api/*", "api");

        tomcat.start();
        System.out.println("Quiz App запущено: http://localhost:" + config.port() + "/api");
        if (config.teacherInviteCode() == null || config.teacherInviteCode().isBlank()) {
            System.out.println("Увага: TEACHER_INVITE_CODE не задано — реєстрацію викладачів вимкнено.");
        }
        System.out.println("Дані зберігаються в пам'яті й зникнуть після зупинки сервера.");
        tomcat.getServer().await();
    }
}
