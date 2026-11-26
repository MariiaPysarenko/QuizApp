package com.quizapp.importing.googleforms;

import java.util.Arrays;
import java.util.List;

/**
 * Допоміжні методи для створення експорту Google Forms у тестах.
 */
final class GoogleFormsTestData {

    private GoogleFormsTestData() {
    }

    static GoogleFormExport form(String title, GoogleFormExport.Item... items) {
        return new GoogleFormExport("google-forms", "form-1", title, "Опис форми", true, Arrays.asList(items));
    }

    static GoogleFormExport.Item item(String type, String title, GoogleFormExport.Choice... choices) {
        return new GoogleFormExport.Item(type, title, 1, null, choices.length == 0 ? null : List.of(choices));
    }

    static GoogleFormExport.Item multipleChoice(String title, GoogleFormExport.Choice... choices) {
        return item("MULTIPLE_CHOICE", title, choices);
    }

    static GoogleFormExport.Choice right(String value) {
        return new GoogleFormExport.Choice(value, true);
    }

    static GoogleFormExport.Choice wrong(String value) {
        return new GoogleFormExport.Choice(value, false);
    }
}
