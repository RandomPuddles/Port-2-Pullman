# Architecture

## Overview

The system has four responsibilities: storing reminders, scheduling evaluations, asking Gemini whether a condition is met, and firing a notification when it is. That's it.

```
┌─────────────────────────────────┐
│           UI Layer              │  Jetpack Compose
├─────────────────────────────────┤
│         Domain Layer            │  Reminder, Schedule models
├─────────────────────────────────┤
│           AI Layer              │  GeminiEvaluator
├─────────────────────────────────┤
│        Service Layer            │  WorkManager + NotificationService
└─────────────────────────────────┘
```

---

## Layers

### UI Layer

Two screens:

**ReminderMenuScreen** — Lists all reminders. Each shows title, schedule, and active state. Create and delete from here.

**CreateReminderScreen** — Form with:
- Title (text field)
- Condition prompt (multi-line text field — the plain language condition)
- Schedule (interval picker or date/time picker)
- Notification method (TTS toggle, ringing toggle)
- Trigger once toggle (deactivate after first fire)

### Domain Layer

Two model classes. See [DATA_MODEL.md](./DATA_MODEL.md).

### AI Layer

`GeminiEvaluator` — takes a condition prompt string, sends it to Gemini with Google Search grounding enabled, parses the JSON response, and returns a `Boolean`. See [GEMINI.md](./GEMINI.md).

### Service Layer

`EvaluationWorker` — a `CoroutineWorker` managed by WorkManager. Runs on the schedule defined by the reminder. Loads active reminders from Room, calls `GeminiEvaluator` for each, fires `NotificationService` if true. See [BACKGROUND_SERVICE.md](./BACKGROUND_SERVICE.md).

---

## Data Flow

### Creating a Reminder
```
User fills form
    → CreateReminderScreen collects input
    → CreateReminderViewModel builds Reminder object
    → ReminderRepository.save(reminder)
    → Room persists it
    → WorkManager schedules EvaluationWorker for this reminder
```

### Evaluating a Reminder
```
WorkManager triggers EvaluationWorker on schedule
    → ReminderRepository.getById(reminderId)
    → GeminiEvaluator.evaluate(reminder.conditionPrompt)
        → POST to Gemini API with google_search tool enabled
        → Gemini searches web
        → Returns { "result": true } or { "result": false }
    → if true:
        → NotificationService.fire(reminder)
        → if reminder.triggerOnce: ReminderRepository.deactivate(reminder.id)
```

---

## Key Design Decisions

### Gemini handles everything
There is no condition parsing, no API abstraction, no field/operator/value structure. The user's plain text IS the condition. Gemini with web search knows current weather, news, sports scores, stock prices — anything publicly available on the web. This eliminates the entire API layer that would otherwise be needed.

### WorkManager for scheduling
WorkManager is the correct choice for durable background work on Android. It survives process death and device reboots. Each reminder gets its own `PeriodicWorkRequest` (for interval schedules) or `OneTimeWorkRequest` (for one-shot schedules) tagged with the reminder's ID so it can be cancelled when the reminder is deleted or deactivated.

### Room for persistence
`Reminder` maps to a flat Room entity with no polymorphism. The condition is just a `String` column. No type converters needed.

### JSON-enforced Gemini output
Gemini is configured with `responseMimeType = "application/json"` and a strict schema `{ "result": Boolean }`. This eliminates fragile text parsing and makes the evaluation reliable.
