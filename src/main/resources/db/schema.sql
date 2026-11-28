-- Схема бази даних Quiz App (PostgreSQL).
-- Виконується під час запуску застосунку; IF NOT EXISTS робить повторний запуск безпечним.

CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL
);

CREATE TABLE IF NOT EXISTS quiz (
    id                 BIGSERIAL PRIMARY KEY,
    creator_id         BIGINT REFERENCES users (id),
    title              VARCHAR(255) NOT NULL,
    description        TEXT,
    scoring_mode       VARCHAR(20)  NOT NULL,
    time_limit_seconds BIGINT,
    status             VARCHAR(20)  NOT NULL
);

CREATE TABLE IF NOT EXISTS question (
    id          BIGSERIAL PRIMARY KEY,
    quiz_id     BIGINT  NOT NULL REFERENCES quiz (id) ON DELETE CASCADE,
    position    INTEGER NOT NULL,
    text        TEXT    NOT NULL,
    points      INTEGER NOT NULL,
    explanation TEXT
);

CREATE TABLE IF NOT EXISTS answer_option (
    id          BIGSERIAL PRIMARY KEY,
    question_id BIGINT  NOT NULL REFERENCES question (id) ON DELETE CASCADE,
    position    INTEGER NOT NULL,
    text        TEXT    NOT NULL,
    is_correct  BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS attempt (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES users (id),
    quiz_id      BIGINT REFERENCES quiz (id),
    type         VARCHAR(20) NOT NULL,
    status       VARCHAR(20) NOT NULL,
    started_at   TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    score        DOUBLE PRECISION NOT NULL DEFAULT 0,
    max_score    DOUBLE PRECISION NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS submitted_answer (
    id                 BIGSERIAL PRIMARY KEY,
    attempt_id         BIGINT  NOT NULL REFERENCES attempt (id) ON DELETE CASCADE,
    position           INTEGER NOT NULL,
    question_id        BIGINT  NOT NULL REFERENCES question (id),
    selected_option_id BIGINT REFERENCES answer_option (id),
    is_correct         BOOLEAN NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_attempt_user_status ON attempt (user_id, status, completed_at);
CREATE INDEX IF NOT EXISTS idx_question_quiz ON question (quiz_id, position);
CREATE INDEX IF NOT EXISTS idx_option_question ON answer_option (question_id, position);
CREATE INDEX IF NOT EXISTS idx_answer_attempt ON submitted_answer (attempt_id, position);
