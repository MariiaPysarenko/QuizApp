package com.quizapp.web.dto;

import com.quizapp.common.ValidationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Тіло запиту з відповідями: список пар «запитання — обраний варіант». */
public record SubmitAnswersRequest(List<AnswerRequest> answers) {

    /** @param optionId обраний варіант або {@code null}, якщо запитання пропущено */
    public record AnswerRequest(Long questionId, Long optionId) {
    }

    /** id запитання → id варіанта; пропущені запитання не потрапляють у мапу. */
    public Map<Long, Long> toMap() {
        Map<Long, Long> result = new HashMap<>();
        for (AnswerRequest answer : answers == null ? List.<AnswerRequest>of() : answers) {
            if (answer.questionId() == null) {
                throw new ValidationException("У відповіді не вказано questionId");
            }
            if (result.containsKey(answer.questionId())) {
                throw new ValidationException("Повторна відповідь на запитання " + answer.questionId());
            }
            if (answer.optionId() != null) {
                result.put(answer.questionId(), answer.optionId());
            }
        }
        return result;
    }
}
