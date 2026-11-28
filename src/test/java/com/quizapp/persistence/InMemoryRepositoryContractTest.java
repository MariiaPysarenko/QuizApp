package com.quizapp.persistence;

import com.quizapp.persistence.memory.InMemoryAttemptRepository;
import com.quizapp.persistence.memory.InMemoryQuizRepository;
import com.quizapp.persistence.memory.InMemoryUserRepository;

/**
 * Контракт репозиторіїв для реалізації в пам'яті.
 */
class InMemoryRepositoryContractTest extends RepositoryContractTest {

    @Override
    protected void createRepositories() {
        users = new InMemoryUserRepository();
        quizzes = new InMemoryQuizRepository();
        attempts = new InMemoryAttemptRepository();
    }
}
