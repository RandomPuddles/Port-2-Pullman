# EventTriggerAlarm

An Android alarm app that triggers based on real-world conditions described in plain language. Instead of setting a time, you describe what should happen — "alert me when it stops raining and temperature is above 70°F" — and the app evaluates that condition on a schedule using Gemini with Google Search.

---

## How It Works

1. User creates a reminder with a plain-text condition and a schedule (interval or one-shot)
2. A background service evaluates the condition on that schedule
3. Gemini searches the web and returns true or false
4. If true, the app fires a notification (TTS voice or phone ringing)

That's the entire system.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Async | Coroutines + Flow |
| Database | Room |
| Preferences | DataStore |
| AI Evaluation | Gemini SDK (`gemini-2.0-flash`) |
| Web Search | Google Search Grounding (built into Gemini) |
| DI | Hilt |
| Background | WorkManager |
| IDE | Android Studio |
| Min SDK | 26 |

---

## Documentation

| File | Description |
|---|---|
| [ARCHITECTURE.md](./ARCHITECTURE.md) | System design and data flow |
| [DATA_MODEL.md](./DATA_MODEL.md) | Reminder model, Room schema, JSON |
| [GEMINI.md](./GEMINI.md) | Gemini integration, evaluation logic, JSON enforcement |
| [BACKGROUND_SERVICE.md](./BACKGROUND_SERVICE.md) | Scheduling, WorkManager, notification |
| [IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md) | Ordered build steps for MVP |
| [TECH_STACK.md](./TECH_STACK.md) | All Gradle dependencies and setup |

---

## Project Structure

```
app/src/main/java/com/example/eventtriggeralarm/
├── data/
│   ├── db/                  # Room database, DAO, entity
│   ├── datastore/           # DataStore preferences
│   └── repository/          # ReminderRepository
├── domain/
│   └── model/               # Reminder, Schedule, NotificationMethod
├── ai/
│   └── GeminiEvaluator.kt   # Gemini + web search condition evaluation
├── service/
│   └── EvaluationWorker.kt  # WorkManager worker
├── notification/
│   └── NotificationService.kt
├── ui/
│   ├── menu/                # Reminder list screen
│   ├── create/              # Create/edit reminder screen
│   └── theme/               # Compose theme
└── di/                      # Hilt modules
```

---

## Setup

1. Clone the repo and open in Android Studio
2. Add to `local.properties`:
```
GEMINI_API_KEY=your_key_here
```
3. Sync Gradle and run on a device or emulator (API 26+)
