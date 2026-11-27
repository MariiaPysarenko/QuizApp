package com.quizapp.web;

import com.quizapp.common.ValidationException;

/**
 * Читання тіла запиту як об'єкта потрібного типу.
 * У застосунку це JSON (Jackson), у тестах — готовий об'єкт.
 */
@FunctionalInterface
public interface BodyReader {

    <T> T read(Class<T> type);

    /** Тіло відсутнє: спроба його прочитати — помилка клієнта. */
    BodyReader EMPTY = new BodyReader() {
        @Override
        public <T> T read(Class<T> type) {
            throw new ValidationException("Тіло запиту відсутнє");
        }
    };

    /** Тіло, вже представлене об'єктом (для тестів). */
    static BodyReader of(Object body) {
        if (body == null) {
            return EMPTY;
        }
        return new BodyReader() {
            @Override
            public <T> T read(Class<T> type) {
                if (!type.isInstance(body)) {
                    throw new ValidationException("Очікувалося тіло типу " + type.getSimpleName());
                }
                return type.cast(body);
            }
        };
    }
}
