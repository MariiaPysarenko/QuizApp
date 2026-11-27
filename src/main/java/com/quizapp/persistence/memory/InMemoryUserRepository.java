package com.quizapp.persistence.memory;

import com.quizapp.auth.UserRepository;
import com.quizapp.domain.User;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Зберігання користувачів у пам'яті — для тестів і для запуску застосунку до підключення бази даних.
 */
public class InMemoryUserRepository implements UserRepository {

    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public User save(User user) {
        User stored = user.id() != null ? user : new User(
                sequence.incrementAndGet(), user.email(), user.passwordHash(), user.role(), user.createdAt());
        users.put(stored.id(), stored);
        return stored;
    }

    @Override
    public Optional<User> findById(long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return users.values().stream()
                .filter(user -> user.email().equals(email))
                .findFirst();
    }
}
