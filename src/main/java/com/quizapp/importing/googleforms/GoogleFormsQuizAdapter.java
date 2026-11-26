package com.quizapp.importing.googleforms;

import com.quizapp.domain.Quiz;
import com.quizapp.importing.ImportResult;
import com.quizapp.importing.QuizImport;
import com.quizapp.importing.SkippedItem;
import com.quizapp.quiz.QuizBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Патерн Adapter (структурний).
 * <p>
 * Пристосовує експорт Google Forms ({@link GoogleFormExport}) до інтерфейсу {@link QuizImport},
 * з яким працює система. Решта коду нічого не знає про формат Google Forms.
 * Квіз збирається через {@link QuizBuilder}, тому діють ті самі правила, що й при ручному створенні.
 */
public class GoogleFormsQuizAdapter implements QuizImport {

    static final String DEFAULT_TITLE = "Квіз з Google Forms";

    /** Типи з одним правильним варіантом — їх можна перенести без втрат. */
    private static final Set<String> SUPPORTED_TYPES = Set.of("MULTIPLE_CHOICE", "LIST");

    /** Елементи оформлення, які не є запитаннями: пропускаються мовчки. */
    private static final Set<String> LAYOUT_TYPES = Set.of("SECTION_HEADER", "PAGE_BREAK", "IMAGE", "VIDEO");

    private static final int MIN_CHOICES = 2;

    private final GoogleFormExport export;

    public GoogleFormsQuizAdapter(GoogleFormExport export) {
        this.export = export;
    }

    @Override
    public ImportResult toQuiz(long creatorId) {
        QuizBuilder builder = new QuizBuilder()
                .creatorId(creatorId)
                .title(isBlank(export.title()) ? DEFAULT_TITLE : export.title())
                .description(export.description());

        List<SkippedItem> skipped = new ArrayList<>();
        int imported = 0;
        List<GoogleFormExport.Item> items = export.items() == null ? List.of() : export.items();

        for (int i = 0; i < items.size(); i++) {
            GoogleFormExport.Item item = items.get(i);
            if (LAYOUT_TYPES.contains(item.type())) {
                continue;
            }
            String reason = whyNotSupported(item);
            if (reason != null) {
                skipped.add(new SkippedItem(i + 1, item.title(), reason));
                continue;
            }
            addQuestion(builder, item);
            imported++;
        }

        Quiz quiz = imported > 0 ? builder.build() : null;
        return new ImportResult(quiz, skipped);
    }

    /** Повертає причину, чому запитання не можна імпортувати, або {@code null}, якщо можна. */
    private static String whyNotSupported(GoogleFormExport.Item item) {
        if (!SUPPORTED_TYPES.contains(item.type())) {
            return "непідтримуваний тип запитання: " + item.type();
        }
        if (isBlank(item.title())) {
            return "порожній текст запитання";
        }
        List<GoogleFormExport.Choice> choices = item.choices() == null ? List.of() : item.choices();
        if (choices.size() < MIN_CHOICES) {
            return "менше двох варіантів відповіді";
        }
        long correct = choices.stream().filter(GoogleFormExport.Choice::isCorrect).count();
        if (correct == 0) {
            return "не позначено правильну відповідь (форма не в режимі «Тест»?)";
        }
        if (correct > 1) {
            return "кілька правильних відповідей";
        }
        return null;
    }

    private static void addQuestion(QuizBuilder builder, GoogleFormExport.Item item) {
        builder.question(item.title());
        for (GoogleFormExport.Choice choice : item.choices()) {
            builder.option(choice.value(), choice.isCorrect());
        }
        if (item.points() != null && item.points() > 0) {
            builder.points(item.points());
        }
        if (!isBlank(item.feedbackIncorrect())) {
            builder.explanation(item.feedbackIncorrect());
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
