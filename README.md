# Quiz App — система онлайн-тестування

Вебзастосунок для створення квізів (вручну або імпортом із Google Forms),
їх проходження з автоматичним оцінюванням і роботи над помилками.

Навчальний пет-проєкт з дисципліни «Програмування на Java».

## Документація

- [SDLC](docs/SDLC.md) — методологія, фази, таймлайн
- [SDD](docs/SDD.md) — вимоги, архітектура, модель даних, API, патерни

## Технології

Java 21, Maven, Jakarta Servlet (вбудований Tomcat), Jackson, JDBC + PostgreSQL, JUnit 5.

## Запуск тестів

```bash
mvn test
```

Звіт про покриття: `target/site/jacoco/index.html`.

## База даних

Застосунок працює з PostgreSQL, якщо задано змінну `DB_URL`; без неї дані зберігаються в пам'яті
(зручно для швидкого запуску й демо). Таблиці створюються автоматично під час запуску (`src/main/resources/db/schema.sql`).

Підготовка бази (один раз, у pgAdmin → Query Tool від імені `postgres`):

```sql
CREATE USER quizapp WITH PASSWORD 'quizapp';
CREATE DATABASE quizapp OWNER quizapp;
CREATE DATABASE quizapp_test OWNER quizapp;
```

| Змінна | Приклад | Для чого |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/quizapp` | база застосунку |
| `DB_USER`, `DB_PASSWORD` | `quizapp`, `quizapp` | облікові дані |
| `TEST_DB_URL`, `TEST_DB_USER`, `TEST_DB_PASSWORD` | `jdbc:postgresql://localhost:5432/quizapp_test`, … | тести репозиторіїв на PostgreSQL; без них ці тести пропускаються |

Тестова база очищається перед кожним тестом — не вказуйте в `TEST_DB_URL` робочу базу.

## Запуск застосунку

PowerShell, у корені проєкту:

```powershell
$env:TEACHER_INVITE_CODE = "teacher-2026"   # код для реєстрації викладачів
$env:DB_URL = "jdbc:postgresql://localhost:5432/quizapp"   # без цих трьох рядків — дані в пам'яті
$env:DB_USER = "quizapp"
$env:DB_PASSWORD = "quizapp"
mvn compile exec:java
```

Сервер стартує на `http://localhost:8080/api` (порт можна змінити змінною `PORT`).

В IntelliJ IDEA: запустіть клас `com.quizapp.Main`, а змінні задайте в
Run → Edit Configurations → Environment variables, наприклад:
`TEACHER_INVITE_CODE=teacher-2026;DB_URL=jdbc:postgresql://localhost:5432/quizapp;DB_USER=quizapp;DB_PASSWORD=quizapp`.

## Демонстрація

Коли сервер запущено, в іншому вікні PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File demo\demo.ps1
```

Скрипт проходить увесь сценарій: реєстрація, імпорт із Google Forms, створення та публікація квізу,
проходження, перевірка прав доступу, робота над помилками, історія спроб.

## API

| Метод | Шлях | Хто |
|---|---|---|
| POST | `/api/auth/register` | гість (викладач — з `inviteCode`) |
| POST | `/api/auth/login`, `/api/auth/logout` | гість / користувач |
| GET | `/api/me` | користувач |
| GET | `/api/quizzes`, `/api/quizzes/{id}` | користувач |
| POST | `/api/quizzes` | викладач |
| POST | `/api/quizzes/{id}/publish` | автор квізу |
| POST | `/api/quizzes/import/google-forms` | викладач |
| POST | `/api/quizzes/{id}/attempts` | студент |
| POST | `/api/attempts/{id}/submit` | власник спроби |
| GET | `/api/me/attempts` | студент |
| GET | `/api/me/practice`, POST `/api/me/practice/submit` | студент |

Помилки повертаються як `{"error": "..."}` з кодами 400, 401, 403, 404, 405, 409.
