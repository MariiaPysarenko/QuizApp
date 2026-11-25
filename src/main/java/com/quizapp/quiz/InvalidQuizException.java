package com.quizapp.quiz;

/**
 * Квіз не можна створити, бо дані некоректні (немає назви, запитань, правильної відповіді тощо).
 */
public class InvalidQuizException extends RuntimeException {

    public InvalidQuizException(String message) {
        super(message);
    }
}
