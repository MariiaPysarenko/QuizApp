package com.quizapp.persistence.jdbc;

/**
 * Помилка роботи з базою даних. Перетворюється маршрутизатором на HTTP 500,
 * а деталі (SQL, стек) потрапляють лише в журнал сервера.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
