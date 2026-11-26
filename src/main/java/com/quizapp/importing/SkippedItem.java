package com.quizapp.importing;

/**
 * Запитання, яке не вдалося імпортувати.
 *
 * @param position номер елемента у вихідній формі (з 1)
 * @param title    текст запитання
 * @param reason   причина пропуску
 */
public record SkippedItem(int position, String title, String reason) {
}
