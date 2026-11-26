package com.quizapp.importing.googleforms;

import java.util.List;

/**
 * JSON-файл, який створює скрипт {@code tools/google-forms-export/Code.gs}.
 * Це «чужий» формат (adaptee), який {@link GoogleFormsQuizAdapter} перетворює на квіз.
 */
public record GoogleFormExport(String source,
                               String formId,
                               String title,
                               String description,
                               boolean isQuiz,
                               List<Item> items) {

    /**
     * Елемент форми: запитання будь-якого типу, заголовок секції, зображення тощо.
     *
     * @param type    тип елемента в Google Forms: MULTIPLE_CHOICE, LIST, CHECKBOX, TEXT, ...
     * @param points  бали за запитання в режимі тесту; може бути {@code null} або 0
     * @param choices варіанти відповіді; {@code null} для запитань без варіантів
     */
    public record Item(String type,
                       String title,
                       Integer points,
                       String feedbackIncorrect,
                       List<Choice> choices) {
    }

    public record Choice(String value, boolean isCorrect) {
    }
}
