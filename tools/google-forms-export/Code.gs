/**
 * Експорт Google Form у JSON для імпорту в Quiz App.
 *
 * Як користуватися:
 *   1. Відкрийте форму → ⋮ → «Редактор скриптів» (або «Розширення» → «Apps Script»).
 *   2. Вставте цей код, збережіть, виберіть функцію exportFormToJson і натисніть «Виконати».
 *   3. Під час першого запуску дозвольте доступ до форми та Google Drive.
 *   4. У журналі виконання з'явиться посилання на JSON-файл у вашому Google Drive.
 *   5. Завантажте файл і передайте його в Quiz App (POST /api/quizzes/import/google-forms).
 *
 * Правильні відповіді та бали експортуються, лише якщо форма в режимі «Тест»
 * (Налаштування → «Зробити тестом») і правильні варіанти позначені в ключі відповідей.
 */
function exportFormToJson() {
  var form = FormApp.getActiveForm();

  var data = {
    source: 'google-forms',
    formId: form.getId(),
    title: form.getTitle(),
    description: form.getDescription(),
    isQuiz: form.isQuiz(),
    items: form.getItems().map(exportItem_)
  };

  var json = JSON.stringify(data, null, 2);
  var file = DriveApp.createFile(form.getTitle() + '.quizapp.json', json, MimeType.PLAIN_TEXT);
  Logger.log('Експортовано елементів: ' + data.items.length);
  Logger.log('Файл: ' + file.getUrl());
}

/** Перетворює елемент форми на об'єкт JSON. */
function exportItem_(item) {
  var type = item.getType();
  var result = {
    type: String(type),
    title: item.getTitle()
  };

  if (type === FormApp.ItemType.MULTIPLE_CHOICE) {
    return addChoices_(result, item.asMultipleChoiceItem());
  }
  if (type === FormApp.ItemType.LIST) {
    return addChoices_(result, item.asListItem());
  }
  if (type === FormApp.ItemType.CHECKBOX) {
    return addChoices_(result, item.asCheckboxItem());
  }
  // Інші типи (текст, шкала, сітка, секції) експортуються лише з типом і назвою —
  // Quiz App покаже їх у звіті як пропущені.
  return result;
}

/** Додає бали, пояснення та варіанти відповіді для запитань з вибором. */
function addChoices_(result, item) {
  var feedback = item.getFeedbackForIncorrect();
  result.points = item.getPoints();
  result.feedbackIncorrect = feedback ? feedback.getText() : null;
  result.choices = item.getChoices().map(function (choice) {
    return {
      value: choice.getValue(),
      isCorrect: choice.isCorrectAnswer()
    };
  });
  return result;
}
