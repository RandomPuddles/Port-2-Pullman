# Implementation Plan

Ordered build steps for the MVP. Each phase produces something runnable. Do not skip phases.

---

## Phase 1 — Project Setup

- [ ] Create new Android project (Kotlin, Jetpack Compose, min SDK 26)
- [ ] Add all Gradle dependencies from [TECH_STACK.md](./TECH_STACK.md)
- [ ] Add `GEMINI_API_KEY` to `local.properties` and expose via `BuildConfig`
- [ ] Create `App : Application()` with `@HiltAndroidApp` and notification channel setup
- [ ] Create package structure as defined in [README.md](./README.md)
- [ ] Verify project builds and runs on emulator

---

## Phase 2 — Domain & Data Layer

- [ ] Define `Reminder`, `Schedule`, `NotificationMethod` domain models
- [ ] Define `ReminderEntity` Room entity
- [ ] Implement `ReminderDao`
- [ ] Create `AppDatabase`
- [ ] Implement `ReminderRepository` with domain ↔ entity mapping
- [ ] Wire `AppDatabase` and `ReminderRepository` in Hilt module
- [ ] Wire `DataStore` in Hilt module
- [ ] Write unit test: save a reminder, load it back, verify all fields

---

## Phase 3 — Gemini Integration

- [ ] Implement `GeminiEvaluator` with JSON schema enforcement and Google Search grounding
- [ ] Wire `GeminiEvaluator` in Hilt module
- [ ] Manual test: call `evaluate("is it currently daytime?")` and verify true/false response
- [ ] Manual test: call `evaluate("is it raining in London right now?")` and verify response matches reality

---

## Phase 4 — Background Service

- [ ] Implement `EvaluationWorker` as `@HiltWorker`
- [ ] Implement `SchedulerService` for WorkManager scheduling and cancellation
- [ ] Implement `NotificationService` with TTS and ringing
- [ ] Add `RECEIVE_BOOT_COMPLETED` receiver to restart WorkManager after reboot
- [ ] Add required permissions to `AndroidManifest.xml`
- [ ] Manual test: save a reminder with a 15-minute interval, verify WorkManager schedules it, verify it evaluates and notifies

---

## Phase 5 — UI

### Reminder List Screen
- [ ] `ReminderMenuViewModel` — exposes `Flow<List<Reminder>>` from repository
- [ ] `ReminderMenuScreen` — list of reminder cards showing title, schedule, active state
- [ ] Delete swipe gesture or delete button on each card
- [ ] Empty state when no reminders exist
- [ ] FAB to navigate to create screen

### Create Reminder Screen
- [ ] `CreateReminderViewModel` — manages form state and save action
- [ ] `CreateReminderScreen`:
  - [ ] Title text field
  - [ ] Condition prompt multi-line text field with placeholder ("e.g. is it not raining and above 70°F?")
  - [ ] Schedule picker — toggle between Interval and OneShot
    - Interval: dropdown or slider (15min / 30min / 1hr / 3hr / 6hr / 12hr / 24hr)
    - OneShot: date + time picker
  - [ ] Notification method toggles (Voice Output, Ringing)
  - [ ] Trigger Once toggle
  - [ ] Save button → saves to Room → schedules WorkManager → navigates back

### Navigation
- [ ] Compose Navigation with two destinations: Menu and Create
- [ ] Pass reminder ID to Create for edit flow

---

## Phase 6 — Settings

- [ ] Settings screen accessible from menu (top bar icon)
- [ ] Gemini API key input field (obscured, stored in DataStore)
- [ ] Default interval preference

---

## Phase 7 — Polish

- [ ] Request `POST_NOTIFICATIONS` permission at runtime on Android 13+
- [ ] Handle edge cases:
  - Empty title → validation error
  - Empty condition prompt → validation error
  - OneShot time in the past → validation error
  - No internet on evaluation → WorkManager retries automatically
- [ ] Test WorkManager surviving app kill: `adb shell am kill com.example.eventtriggeralarm`
- [ ] Test on physical device
- [ ] Basic error logging with Timber
