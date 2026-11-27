# Демонстрація Quiz App: повний сценарій через REST API.
#
# 1. Запустіть сервер (в окремому вікні PowerShell, у корені проєкту):
#      $env:TEACHER_INVITE_CODE = "teacher-2026"
#      mvn compile exec:java
# 2. Запустіть цей скрипт:
#      powershell -ExecutionPolicy Bypass -File demo\demo.ps1
#
# Скрипт можна запускати кілька разів поспіль: щоразу створюються нові користувачі.

param(
    [string]$BaseUrl = "http://localhost:8080/api",
    [string]$InviteCode = "teacher-2026"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$root = Split-Path -Parent $PSScriptRoot
$stamp = Get-Date -Format "HHmmss"
$password = "secret-password"

# ---------- допоміжні функції ----------

function Step($title) {
    Write-Host ""
    Write-Host "=== $title ===" -ForegroundColor Cyan
}

function Call($Method, $Path, $Session, $Body = $null) {
    $params = @{
        Method      = $Method
        Uri         = "$BaseUrl$Path"
        WebSession  = $Session
        ContentType = "application/json; charset=utf-8"
    }
    if ($null -ne $Body) {
        $json = if ($Body -is [string]) { $Body } else { $Body | ConvertTo-Json -Depth 10 }
        # Тіло передається байтами UTF-8, щоб українські літери не зіпсувалися у Windows PowerShell 5.1.
        $params.Body = [System.Text.Encoding]::UTF8.GetBytes($json)
    }
    Invoke-RestMethod @params
}

function ExpectError($Description, [scriptblock]$Action) {
    try {
        & $Action | Out-Null
        Write-Host "  $Description — помилки не було, а мала бути!" -ForegroundColor Red
    } catch {
        $status = [int]$_.Exception.Response.StatusCode
        Write-Host "  $Description -> HTTP $status $($_.ErrorDetails.Message)" -ForegroundColor Yellow
    }
}

function OptionId($Question, $Text) {
    ($Question.options | Where-Object { $_.text -eq $Text }).id
}

function ShowResult($Result) {
    Write-Host "  Результат: $($Result.score) / $($Result.maxScore) балів ($($Result.percent)%)" -ForegroundColor Green
    foreach ($q in $Result.questions) {
        $mark = if ($q.correct) { "+" } else { "-" }
        $line = "   [$mark] $($q.text) ($($q.points) б.)"
        if (-not $q.correct -and $q.explanation) { $line += " — підказка: $($q.explanation)" }
        Write-Host $line
    }
}

$teacher = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$student = New-Object Microsoft.PowerShell.Commands.WebRequestSession

# ---------- 1. Користувачі ----------

Step "Реєстрація та вхід"
$teacherEmail = "teacher$stamp@example.com"
$studentEmail = "student$stamp@example.com"

Call POST "/auth/register" $teacher @{ email = $teacherEmail; password = $password; role = "TEACHER"; inviteCode = $InviteCode } | Out-Null
Call POST "/auth/login" $teacher @{ email = $teacherEmail; password = $password } | Out-Null
Call POST "/auth/register" $student @{ email = $studentEmail; password = $password; role = "STUDENT" } | Out-Null
Call POST "/auth/login" $student @{ email = $studentEmail; password = $password } | Out-Null

$me = Call GET "/me" $teacher
Write-Host "  Викладач: $($me.email) ($($me.role))"
$me = Call GET "/me" $student
Write-Host "  Студент:  $($me.email) ($($me.role))"

ExpectError "Студент реєструється викладачем без коду" {
    Call POST "/auth/register" $student @{ email = "hacker$stamp@example.com"; password = $password; role = "TEACHER" }
}
ExpectError "Вхід з невірним паролем" {
    Call POST "/auth/login" (New-Object Microsoft.PowerShell.Commands.WebRequestSession) @{ email = $studentEmail; password = "wrong-password" }
}

# ---------- 2. Імпорт із Google Forms ----------

Step "Імпорт із Google Forms (Adapter)"
$export = Get-Content -Raw -Encoding UTF8 (Join-Path $root "tools\google-forms-export\example.quizapp.json")
$imported = Call POST "/quizzes/import/google-forms" $teacher $export
Write-Host "  Квіз «$($imported.quiz.title)» (id $($imported.quiz.id), $($imported.quiz.status))"
Write-Host "  Імпортовано запитань: $($imported.importedCount)"
foreach ($s in $imported.skipped) {
    Write-Host "  Пропущено #$($s.position) «$($s.title)»: $($s.reason)" -ForegroundColor Yellow
}
Call POST "/quizzes/$($imported.quiz.id)/publish" $teacher | Out-Null

# ---------- 3. Створення квізу вручну ----------

Step "Викладач створює квіз (Builder) і публікує його"
$quiz = Call POST "/quizzes" $teacher @{
    title            = "ООП у Java"
    description      = "Демонстраційний квіз"
    scoringMode      = "WEIGHTED"
    timeLimitSeconds = 600
    questions        = @(
        @{ text = "Який патерн будує складний об'єкт покроково?"; points = 2; explanation = "Builder"
           options = @(@{ text = "Builder"; correct = $true }, @{ text = "Adapter"; correct = $false }) },
        @{ text = "Який патерн пристосовує чужий інтерфейс?"; points = 1; explanation = "Adapter"
           options = @(@{ text = "Strategy"; correct = $false }, @{ text = "Adapter"; correct = $true }) }
    )
}
Write-Host "  Створено «$($quiz.title)» (id $($quiz.id), $($quiz.status))"
ExpectError "Студент бачить чернетку" { Call GET "/quizzes/$($quiz.id)" $student }
Call POST "/quizzes/$($quiz.id)/publish" $teacher | Out-Null
Write-Host "  Опубліковано"
ExpectError "Студент створює квіз" { Call POST "/quizzes" $student @{ title = "x" } }

# ---------- 4. Проходження ----------

Step "Студент бачить опубліковані квізи"
foreach ($q in (Call GET "/quizzes" $student)) {
    Write-Host "  [$($q.id)] $($q.title) — запитань: $($q.questionCount)"
}

Step "Студент проходить квіз (Strategy рахує бали)"
$attempt = Call POST "/quizzes/$($quiz.id)/attempts" $student
Write-Host "  Спроба $($attempt.attemptId), здати до $($attempt.deadline)"
$q1 = $attempt.quiz.questions[0]
$q2 = $attempt.quiz.questions[1]
Write-Host "  Правильні відповіді студенту не надходять: correct = '$($q1.options[0].correct)'"

$result = Call POST "/attempts/$($attempt.attemptId)/submit" $student @{
    answers = @(
        @{ questionId = $q1.id; optionId = (OptionId $q1 "Builder") },
        @{ questionId = $q2.id; optionId = (OptionId $q2 "Strategy") }
    )
}
ShowResult $result

ExpectError "Повторне надсилання тієї самої спроби" {
    Call POST "/attempts/$($attempt.attemptId)/submit" $student @{ answers = @() }
}

# ---------- 5. Робота над помилками ----------

Step "Робота над помилками"
$practice = Call GET "/me/practice" $student
foreach ($q in $practice.questions) { Write-Host "  У добірці: $($q.text)" }

$p = $practice.questions[0]
$practiceResult = Call POST "/me/practice/submit" $student @{
    answers = @(@{ questionId = $p.id; optionId = (OptionId $p "Adapter") })
}
ShowResult $practiceResult
$left = (Call GET "/me/practice" $student).questions
Write-Host "  Запитань у добірці після практики: $(@($left).Count)"

# ---------- 6. Історія ----------

Step "Історія спроб студента"
foreach ($a in (Call GET "/me/attempts" $student)) {
    Write-Host "  [$($a.type)] $($a.title): $($a.score)/$($a.maxScore) ($($a.percent)%)"
}

Write-Host ""
Write-Host "Демонстрацію завершено." -ForegroundColor Cyan
