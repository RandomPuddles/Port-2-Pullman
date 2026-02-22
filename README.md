# Port 2 Pullman — Eventually (Conditional Alarm System)

An Android alarm app that triggers on real-world **conditions** rather than a fixed time. Conditions are evaluated continuously by a background engine using device sensors, system APIs, and weather data. Alarms can be created manually by browsing a condition catalog, or described in plain language and assembled by AI.

---

## How It Works

1. User creates an alarm with a title and a condition tree (AND/OR combinations of conditions)
2. `AlarmEvaluatorService` runs as a foreground service and polls all enabled alarms on a configurable interval
3. For each alarm, `ConditionTreeEvaluator` walks the condition tree recursively
4. Each `LeafCondition` is evaluated by `RuleEvaluator` using rules defined in `conditions.json`
5. `DataSourceResolver` supplies live values (battery level, weather, time, location, etc.) for each rule
6. When the full tree evaluates to `true`, the alarm fires — showing a notification, optionally ringing, and optionally reading the title aloud via ElevenLabs TTS
7. If `triggerOnce` is set, the alarm is disabled after the first trigger

---

## Two Ways to Create an Alarm

### Manual — Condition Browser
The user browses the condition catalog organized by category (Weather, Device, Time, Location, Recurring, Limits). They select conditions, set numeric values where applicable (e.g. "Temperature above 75°F"), and combine them with AND/OR logic.

### AI-Assisted — Natural Language
The user types a plain-language description ("remind me when it's cold and my battery is low"). `AIViewModel` sends this to **Gemini 2.0 Flash** along with the full list of available condition types from `ConditionRegistry`. Gemini returns a structured JSON object which is parsed into an `AlarmDraft` and handed to the setup screen for review before saving. If the Gemini API key is not configured, a keyword-based fallback simulation is used.

---

## Condition System

### Condition Tree (Boolean Expression Tree)

The condition system implements the **Composite Pattern** as a Boolean Expression Tree:

```
Condition (sealed class)
├── LeafCondition     — a single check (category, type, optional value, negated flag)
└── CompositeCondition — combines children with AND / OR
```

Calling `evaluate()` on the root recursively walks the tree. `CompositeCondition` short-circuits: AND stops on the first `false`, OR stops on the first `true`.

### conditions.json — Single Source of Truth

All built-in condition types are defined in `res/raw/conditions.json`. To add, remove, or edit a condition type, only this file needs to change — no Kotlin code changes required. Each entry defines:

- `type` — unique identifier (e.g. `"temperature_above"`)
- `label` — display name shown in UI
- `hasNum` / `unit` / `placeholder` — UI rendering hints
- `rule` — evaluation rule: `{ source, op, valueRef }`
  - `source` — data-source key resolved by `DataSourceResolver` (e.g. `"weather.temperatureF"`)
  - `op` — comparator: `>`, `<`, `>=`, `<=`, `==`, `!=`, `contains`
  - `valueRef` — either `"user.value"` (uses the value the user entered) or a literal boolean/number
- `requiresPermissions` — Android permissions needed
- `probeKeys` — keys used by the debug probe screen

### Available Condition Categories

| Category | Conditions |
|---|---|
| **Weather** | Temperature above/below, rain expected, snow expected, wind speed above, humidity above |
| **Device** | Battery below/above, connected to WiFi, Bluetooth connected, charging |
| **Time / Date** | Time is (HH:MM), day of week, date is, minutes/seconds from now |
| **Location** | Arrive at location, leave location, within radius |
| **Recurring** | Every X seconds/minutes/hours/days/weeks, X times per day/week |
| **Limits** | Limit triggers per minute/hour/day/week/month |

### Custom Conditions

`CustomCondition` is a separate model with a `statement` (plain text) and a `RefreshFrequency`. The storage layer is implemented (Room entity, DAO, repository). Evaluation integration with the rule engine is pending — custom condition types currently fall through to `false` in `RuleEvaluator`.

---

## Engine

### AlarmEvaluatorService
A `ForegroundService` that runs a coroutine evaluation loop on a configurable interval (`DebugSettings.evalIntervalMs`). On each tick it pre-fetches weather, then evaluates all enabled alarms. On trigger it records to `TriggerHistoryEntity` (used by limit conditions), resets `lastStartedAt` (used by elapsed-time conditions), fires the notification, and optionally disables the alarm.

### DataSourceResolver
Maps data-source keys from `conditions.json` rules to live values. Source key categories:

- `device.*` — `BatteryManager`, `ConnectivityManager`, `BluetoothManager`, `BatteryManager` broadcast
- `time.*` — system clock, elapsed time since `alarm.lastStartedAt`
- `location.*` — `LocationProvider` singleton (active GPS updates via `FusedLocationProviderClient`)
- `weather.*` — `WeatherProvider` cache (Open-Meteo API)
- `recurring.*` — `TriggerHistoryDao` queries
- `limit.*` — `TriggerHistoryDao` queries with time windows

### WeatherProvider
Fetches current weather from **Open-Meteo** (free, no API key required) using the device's GPS coordinates. Data is cached for 10 minutes (`DebugSettings.weatherCacheTtlMs`) to avoid hitting the API on every evaluation tick. Falls back to Pullman, WA coordinates if location is unavailable.

### TTSClient (ElevenLabs)
When `alarm.readout` is true, the alarm title is synthesized to speech via the **ElevenLabs API** (`eleven_flash_v2_5` model, ~75ms latency) and played back through `MediaPlayer` with alarm-stream audio attributes (audible over Do Not Disturb). If `alarm.ring` is also true, TTS is deferred until the user dismisses the alarm notification.

---

## Data Layer

### Room Database (v4)

| Table | Purpose |
|---|---|
| `alarms` | Saved alarms — condition tree stored as JSON string (`conditionTreeJson`) |
| `custom_conditions` | User-created custom conditions with refresh frequency |
| `trigger_history` | Trigger timestamps per alarm — used by limit and recurring conditions |

### Moshi — Condition Tree Serialization
The condition tree (`sealed class Condition`) is serialized to/from JSON using **Moshi** with a custom polymorphic adapter in `MoshiAdapters.kt`. The `type` field discriminates between `"leaf"` and `"composite"` nodes.

---

## Debug System

A built-in debug console is accessible in the app:

- **ConditionProbe** — probes every data-source key from the catalog and shows live values, permission status, and errors. Useful for verifying that sensors and APIs are working before creating alarms.
- **DebugLog** — in-memory ring buffer of log entries shown in the debug console.
- **DebugSettings** — adjustable evaluation interval and weather cache TTL for testing.
- **CrashHandler** — captures uncaught exceptions and writes them to the debug log.

---

## Project Structure

```
app/src/main/java/com/port2pullman/app/
├── App.kt                          # Application class, repository init
├── MainActivity.kt
├── data/
│   ├── AppDatabase.kt              # Room database
│   ├── ConditionRegistry.kt        # Parses conditions.json, single source of truth
│   ├── ConditionRepositoryImpl.kt  # Custom conditions CRUD
│   ├── AlarmRepositoryImpl.kt      # Alarm CRUD
│   ├── Daos.kt                     # Room DAOs
│   ├── Entities.kt                 # Room entities
│   ├── MoshiAdapters.kt            # Polymorphic condition tree serialization
│   └── Repositories.kt             # Repository interfaces
├── model/
│   ├── Alarm.kt                    # Domain model
│   ├── AlarmDraft.kt               # Transient model used during creation
│   ├── Condition.kt                # LeafCondition, CompositeCondition, Operator
│   ├── ConditionMeta.kt            # UI rendering metadata, icon mapping
│   ├── CustomCondition.kt          # Custom condition domain model
│   └── Category.kt                 # Category domain model
├── engine/
│   ├── AlarmEvaluatorService.kt    # Foreground service, evaluation loop
│   ├── ConditionEvaluator.kt       # RuleEvaluator + ConditionTreeEvaluator
│   ├── DataSourceResolver.kt       # Maps source keys to live values
│   ├── WeatherProvider.kt          # Open-Meteo API, 10-min cache
│   ├── LocationProvider.kt         # FusedLocationProviderClient singleton
│   ├── TTSClient.kt                # ElevenLabs TTS
│   ├── NotificationController.kt   # Alarm trigger notifications
│   ├── AlarmDismissReceiver.kt     # Dismiss broadcast receiver
│   └── ExternalDataFetcher.kt      # HTTP stub (Ktor client)
├── ui/
│   ├── home/                       # Alarm list screen
│   ├── setup/                      # Alarm setup + condition browser
│   ├── ai/                         # AI alarm creation dialog + ViewModel
│   └── theme/
├── debug/
│   ├── ConditionProbe.kt           # Data source live probe
│   ├── DebugConsoleScreen.kt       # In-app debug console UI
│   ├── DebugLog.kt                 # In-memory log buffer
│   ├── DebugSettings.kt            # Adjustable debug parameters
│   └── CrashHandler.kt             # Uncaught exception handler
└── navigation/
    └── AppNavigation.kt
```

---

## Setup

### Prerequisites
- Android Studio
- Android device or emulator with **API 34+** (Min SDK 34, Target SDK 36)
- GPS/location enabled on device for weather and location conditions

### API Keys

Create `secrets.properties` in the project root (not committed to version control):

```
GEMINI_API_KEY=your_gemini_key_here
ELEVENLABS_API_KEY=your_elevenlabs_key_here
```

Both keys are optional for basic functionality:
- Without `GEMINI_API_KEY` — AI alarm creation falls back to keyword-based simulation
- Without `ELEVENLABS_API_KEY` — TTS readout is silently skipped; all other alarm features work normally

### Weather
Weather data is sourced from **Open-Meteo** — no API key required.

---

## Tech Stack

| Technology | Purpose |
|---|---|
| Kotlin | Language |
| Jetpack Compose + Material 3 | UI |
| Room (KSP) | Local database |
| Moshi | Condition tree JSON serialization |
| Ktor Client | HTTP (weather, external data) |
| Gemini SDK (`gemini-2.0-flash`) | AI alarm creation |
| ElevenLabs REST API | TTS readout |
| Open-Meteo API | Weather data (free, no key) |
| FusedLocationProviderClient | GPS |
| Coroutines + Flow | Async, reactive UI |
| Compile SDK 36 / Min SDK 34 | Android version |
