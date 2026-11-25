package com.quizapp.attempt.scoring;

import java.util.List;

/**
 * Патерн Strategy (поведінковий): спосіб підрахунку балів.
 * <p>
 * Сервіс проходження квізу не знає, як саме рахуються бали, — він отримує
 * потрібну стратегію відповідно до налаштування квізу.
 */
public interface ScoringStrategy {

    ScoringResult score(List<GivenAnswer> answers);
}
