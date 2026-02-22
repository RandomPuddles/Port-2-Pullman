# Conditional Alarms — API Documentation

> **Tech Stack:** Kotlin · Jetpack Compose · Coroutines/Flow · Room · Moshi · DataStore · Ktor Client · Gemini SDK (`generativeai-google`)

---

## Table of Contents

1. [Internal Interfaces](#1-internal-interfaces)
   - [IAlarmRepository](#11-ialarmrepository)
   - [IConditionRepository](#12-iconditionrepository)
2. [External APIs (Consumed)](#2-external-apis-consumed)
   - [AI Alarm Generation](#21-ai-alarm-generation)
   - [Weather Data](#22-weather-data)
   - [Location Data](#23-location-data)
3. [Data Models](#3-data-models)
4. [Room Database Schema](#4-room-database-schema)

---

## 1. Internal Interfaces

These are the cross-package contracts that connect UI, Storage, and Engine packages. Implementations live in the **Alarm Storage** package; consumers depend only on the interface.

---

### 1.1 IAlarmRepository

**Implementation:** `AlarmRepositoryImpl` (Room + Flow + Moshi)
**Consumers:** `AlarmListViewModel`, `SetupViewModel`, `AlarmEvaluatorService`

| Method | Signature | Description |
|--------|-----------|-------------|
| **getAll** | `fun getAll(): Flow<List<Alarm>>` | Returns a reactive stream of all alarms, ordered by creation date descending. Emits a new list whenever the underlying data changes. |
| **search** | `fun search(query: String): Flow<List<Alarm>>` | Filters alarms where `title` or any condition text contains `query` (case-insensitive). Returns a reactive `Flow`. |
| **getById** | `fun getById(id: Long): Flow<Alarm?>` | Returns a reactive stream for a single alarm. Emits `null` if the alarm does not exist or is deleted. |
| **upsert** | `suspend fun upsert(alarm: Alarm): Long` | Inserts a new alarm or updates an existing one (matched by `id`). Returns the row ID. Conditions are persisted as an ordered JSON array via Moshi — drag-to-reorder changes are saved by passing the reordered list here. |
| **delete** | `suspend fun delete(ids: List<Long>): Unit` | Deletes one or more alarms by ID. Accepts a list to support bulk delete from select mode. |
| **setEnabled** | `suspend fun setEnabled(ids: List<Long>, on: Boolean): Unit` | Enables or disables one or more alarms. Accepts a list to support both single-toggle and bulk select-mode operations without calling in a loop. |

#### Usage Notes

- All `Flow` methods are collected by ViewModels via `viewModelScope` and exposed as `StateFlow` to Compose UI.
- `upsert` is the single write path for both create and edit — there is no separate `reorderConditions` method; condition order is part of the `Alarm` object.
- The Engine calls `setEnabled(listOf(id), false)` to auto-disable trigger-once alarms after they fire.

---

### 1.2 IConditionRepository

**Implementation:** `ConditionRepositoryImpl` (static list + Room + Flow + Moshi)
**Consumers:** `ConditionBrowserViewModel`

| Method | Signature | Description |
|--------|-----------|-------------|
| **getCategories** | `fun getCategories(): Flow<List<Category>>` | Returns a reactive stream of all condition categories. Built-in categories (Weather, Device, Time/Date, Location, Recurring Schedule) are a compiled static list. The Custom category is dynamic and backed by Room. When a custom condition is added/modified/deleted, the Flow re-emits. |
| **upsertCustom** | `suspend fun upsertCustom(condition: CustomCondition): Unit` | Inserts a new custom condition or updates an existing one. The custom condition appears under the "Custom" category and is reusable across alarms. |
| **deleteCustom** | `suspend fun deleteCustom(id: Long): Unit` | Permanently deletes a custom condition by ID. Only user-created custom conditions can be deleted; built-in conditions are immutable. |

#### Usage Notes

- Built-in categories never touch the database — zero migration risk.
- `RefreshFrequency` is stored as a JSON field on `CustomCondition` via Moshi.
- Only the **Condition Browser** package depends on this interface. The Engine evaluates conditions embedded in the `Alarm` object (via `IAlarmRepository`), not by querying conditions separately.

---

## 2. External APIs (Consumed)

These are remote HTTP endpoints called by the app via **Ktor Client**. All network calls are `suspend` functions executed in coroutine scopes.

---

### 2.1 AI Alarm Generation

Called by `AIViewModel` in the **AI Alarm Creator** package.

**Dependency:**

```kotlin
implementation("dev.shreyaspatil.generativeai:generativeai-google:1.5.0")
```

#### Initialization

```kotlin
val generativeModel = GenerativeModel(
    modelName = "gemini-1.5-pro-vision-latest",
    apiKey = BuildConfig.GEMINI_API_KEY
)
```

#### `generateContent(prompt)`

Generate an alarm draft from a natural-language prompt via Google Gemini.

**Usage:**

```kotlin
val response = generativeModel.generateContent(
    "Generate a JSON alarm object for: $userPrompt\n" +
    "Respond with only valid JSON matching this schema: ..." 
)
val json = response.text // raw JSON string
val draft = moshi.adapter(AlarmDraft::class.java).fromJson(json)
```

| Parameter | Type | Description |
|-----------|------|-------------|
| `prompt` | `String` | System + user prompt instructing Gemini to return a structured JSON `AlarmDraft`. |

**Response — `200 OK`**

```json
{
  "title": "Sunny Warm Weekday",
  "conditions": [
    {
      "category": "weather",
      "type": "weather_condition",
      "label": "Weather is Sunny",
      "value": "sunny"
    },
    {
      "category": "weather",
      "type": "temperature_above",
      "label": "Temperature > 70°F",
      "value": 70
    },
    {
      "category": "recurring",
      "type": "day_of_week",
      "label": "Monday–Friday",
      "value": [1, 2, 3, 4, 5]
    }
  ],
  "operators": ["AND", "AND"],
  "readout": false,
  "ring": true,
  "triggerOnce": false
}
```

| Field | Type | Description |
|-------|------|-------------|
| `title` | `String` | Suggested alarm title. |
| `conditions` | `Condition[]` | Ordered list of conditions (see [Condition](#condition) model). |
| `operators` | `String[]` | Boolean operators between conditions. Length = `conditions.length - 1`. Values: `"AND"` or `"OR"`. |
| `readout` | `Boolean` | Whether TTS should read the title aloud when triggered. |
| `ring` | `Boolean` | Whether an audio alarm loop should play when triggered. |
| `triggerOnce` | `Boolean` | Whether the alarm auto-disables after the first trigger. |

**Response — `4xx / 5xx`**

```json
{
  "error": "Unable to parse prompt into alarm conditions."
}
```

| Field | Type | Description |
|-------|------|-------------|
| `error` | `String` | Human-readable error message. |

#### Implementation Notes

- `GenerativeModel.generateContent()` returns raw text; Moshi parses it into `AlarmDraft`.
- Wrapped in `runCatching` → `Result<AlarmDraft>` — never throws.
- The prompt includes a JSON schema so Gemini returns a parseable response.
- On success, the draft is passed as a Moshi-serialized Compose navigation argument to `SetupScreen` for user review before saving.
- API key stored in `secrets.properties` / `BuildConfig.GEMINI_API_KEY` — never committed to source control.
- No Ktor dependency needed — the Gemini SDK handles HTTP internally.

---

### 2.2 Weather Data

Called by `ExternalDataFetcher` in the **Alarm Engine** package.

#### `GET /weather`

Retrieve current weather conditions for a location.

**Request**

```http
GET /weather?lat={latitude}&lon={longitude}&units=imperial HTTP/1.1
Authorization: Bearer <WEATHER_API_KEY>
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `lat` | `Double` | Yes | Latitude coordinate. |
| `lon` | `Double` | Yes | Longitude coordinate. |
| `units` | `String` | No | `"imperial"` or `"metric"`. Default: `"imperial"`. |

**Response — `200 OK`**

```json
{
  "temperature": 72.5,
  "condition": "sunny",
  "humidity": 45,
  "windSpeed": 8.2
}
```

| Field | Type | Description |
|-------|------|-------------|
| `temperature` | `Double` | Current temperature in requested units. |
| `condition` | `String` | Weather condition keyword (e.g., `"sunny"`, `"rain"`, `"snow"`, `"cloudy"`). |
| `humidity` | `Int` | Relative humidity percentage (0–100). |
| `windSpeed` | `Double` | Wind speed in mph or km/h depending on units. |

#### Caching

- Responses are cached in-memory keyed by the condition's `refreshFrequency`.
- A condition with a 10-minute refresh frequency will re-fetch only after the cache expires.

---

### 2.3 Location Data

Called by `ExternalDataFetcher` in the **Alarm Engine** package.

#### `GET /location`

Retrieve the device's current location (used by location-based conditions). May also use Android's `FusedLocationProviderClient` directly instead of a remote API.

**Response — `200 OK`**

```json
{
  "latitude": 46.7298,
  "longitude": -117.1817,
  "accuracy": 15.0
}
```

| Field | Type | Description |
|-------|------|-------------|
| `latitude` | `Double` | Current latitude. |
| `longitude` | `Double` | Current longitude. |
| `accuracy` | `Double` | Accuracy radius in meters. |

#### Caching

- Same caching strategy as Weather — keyed by condition `refreshFrequency`.

---

## 3. Data Models

All models are Kotlin `data class` types serialized via Moshi.

### Alarm

```kotlin
data class Alarm(
    val id: Long = 0,
    val title: String,
    val conditions: List<Condition>,
    val operators: List<Operator>,
    val readout: Boolean = false,
    val ring: Boolean = false,
    val triggerOnce: Boolean = false,
    val enabled: Boolean = true
)
```

### AlarmDraft

Identical structure to `Alarm` but without `id` or `enabled` — used as the AI generation response and Compose navigation argument.

```kotlin
data class AlarmDraft(
    val title: String,
    val conditions: List<Condition>,
    val operators: List<Operator>,
    val readout: Boolean,
    val ring: Boolean,
    val triggerOnce: Boolean
)
```

### Condition

```kotlin
data class Condition(
    val category: String,
    val type: String,
    val label: String,
    val value: Any?
)
```

| Field | Type | Description |
|-------|------|-------------|
| `category` | `String` | One of: `"weather"`, `"device"`, `"time"`, `"location"`, `"recurring"`, `"custom"`. |
| `type` | `String` | Condition subtype identifier (e.g., `"temperature_above"`, `"battery_below"`). |
| `label` | `String` | Human-readable display text. |
| `value` | `Any?` | Numerical threshold, string keyword, list of days, etc. Type depends on `type`. |

### CustomCondition

```kotlin
data class CustomCondition(
    val id: Long = 0,
    val title: String,
    val statement: String,
    val refreshFrequency: RefreshFrequency
)
```

### RefreshFrequency

```kotlin
data class RefreshFrequency(
    val value: Int,
    val unit: TimeUnit
)

enum class TimeUnit {
    SECONDS, MINUTES, HOURS, DAYS
}
```

### Operator

```kotlin
enum class Operator {
    AND, OR
}
```

### Category

```kotlin
data class Category(
    val name: String,
    val conditions: List<Condition>
)
```

---

## 4. Room Database Schema

### Table: `alarms`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Unique alarm identifier. |
| `title` | `TEXT` | `NOT NULL` | Alarm display title. |
| `conditionsJson` | `TEXT` | `NOT NULL` | Ordered `List<Condition>` serialized via Moshi. |
| `operatorsJson` | `TEXT` | `NOT NULL` | `List<Operator>` serialized via Moshi. Length = conditions count − 1. |
| `readout` | `INTEGER` | `NOT NULL DEFAULT 0` | `1` = TTS reads title aloud on trigger. |
| `ring` | `INTEGER` | `NOT NULL DEFAULT 0` | `1` = audio alarm loop on trigger. |
| `triggerOnce` | `INTEGER` | `NOT NULL DEFAULT 0` | `1` = auto-disable after first trigger. |
| `enabled` | `INTEGER` | `NOT NULL DEFAULT 1` | `1` = alarm is active and monitored by Engine. |

### Table: `custom_conditions`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Unique condition identifier. |
| `title` | `TEXT` | `NOT NULL` | Custom condition display title. |
| `statement` | `TEXT` | `NOT NULL` | Boolean expression / condition statement. |
| `refreshFreqJson` | `TEXT` | `NOT NULL` | `RefreshFrequency` serialized via Moshi. |
