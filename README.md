# Quiz App — система онлайн-тестування

Вебзастосунок для створення квізів (вручну або імпортом із Google Forms),
їх проходження з автоматичним оцінюванням і роботи над помилками.

Навчальний пет-проєкт з дисципліни «Програмування на Java».

## Документація

- [SDLC](docs/SDLC.md) — методологія, фази, таймлайн
- [SDD](docs/SDD.md) — вимоги, архітектура, модель даних, API, патерни

## Технології

Java 21, Maven, Jakarta Servlet (вбудований Tomcat), Jackson, JUnit 5. Далі — JDBC і PostgreSQL.

## Запуск тестів

```bash
mvn test
```

Звіт про покриття: `target/site/jacoco/index.html`.

## Запуск застосунку

Потрібна лише Java 21 і Maven. Поки що дані зберігаються в пам'яті й зникають після зупинки сервера.

PowerShell, у корені проєкту:

```powershell
$env:TEACHER_INVITE_CODE = "teacher-2026"   # код для реєстрації викладачів
mvn compile exec:java
```

Сервер стартує на `http://localhost:8080/api` (порт можна змінити змінною `PORT`).

В IntelliJ IDEA: запустіть клас `com.quizapp.Main`, а змінну `TEACHER_INVITE_CODE` задайте в
Run → Edit Configurations → Environment variables.

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
