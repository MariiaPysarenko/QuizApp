package com.quizapp.persistence.memory;

import com.quizapp.attempt.AttemptRepository;
import com.quizapp.domain.Attempt;
import com.quizapp.domain.AttemptStatus;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Зберігання спроб у пам'яті — для тестів і для запуску застосунку до підключення бази даних.
 */
public class InMemoryAttemptRepository implements AttemptRepository {

    private final Map<Long, Attempt> attempts = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public Attempt save(Attempt attempt) {
        Attempt stored = attempt.id() != null ? attempt : new Attempt(
                sequence.incrementAndGet(), attempt.userId(), attempt.quizId(), attempt.type(),
                attempt.status(), attempt.startedAt(), attempt.completedAt(),
                attempt.score(), attempt.maxScore(), attempt.answers());
        attempts.put(stored.id(), stored);
        return stored;
    }

    @Override
    public Optional<Attempt> findById(long id) {
        return Optional.ofNullable(attempts.get(id));
    }

    @Override
    public List<Attempt> findCompletedByUser(long userId) {
        return attempts.values().stream()
                .filter(attempt -> attempt.userId() == userId)
                .filter(attempt -> attempt.status() == AttemptStatus.COMPLETED)
                .sorted(Comparator.comparing(Attempt::completedAt).thenComparing(Attempt::id))
                .toList();
    }
}
