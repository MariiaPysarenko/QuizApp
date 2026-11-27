package com.quizapp.web.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.quizapp.common.ValidationException;
import com.quizapp.web.BodyReader;

/**
 * Перетворення між JSON і об'єктами Java (бібліотека Jackson).
 */
public final class JsonMapper {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /** Тіло запиту, яке розбирається з JSON у потрібний тип лише тоді, коли обробник його попросить. */
    public BodyReader reader(String json) {
        if (json == null || json.isBlank()) {
            return BodyReader.EMPTY;
        }
        return new BodyReader() {
            @Override
            public <T> T read(Class<T> type) {
                try {
                    return mapper.readValue(json, type);
                } catch (JsonProcessingException e) {
                    throw new ValidationException("Некоректний JSON: " + e.getOriginalMessage());
                }
            }
        };
    }

    public String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Не вдалося перетворити відповідь на JSON", e);
        }
    }
}
