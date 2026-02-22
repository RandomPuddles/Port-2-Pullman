# Tech Stack

This document is the single source of truth for all technologies used in the project. When in doubt about which library to use for a given task, refer here first.

---

## Language & IDE

| Technology | Version | Purpose |
|---|---|---|
| Kotlin | Latest stable | Primary language |
| Android Studio | Hedgehog+ | IDE |
| Min SDK | 26 (Android 8.0) | Minimum supported device |
| Target SDK | 34 (Android 14) | Target device |

---

## UI

| Technology | Purpose |
|---|---|
| Jetpack Compose | All UI — screens, components, condition tiles |
| Compose Navigation | Screen routing (AlarmMenuScreen ↔ AlarmOptionsScreen) |
| Material 3 | Design system and components |

No XML layouts. No Fragments. All UI is Compose only.

---

## Async

| Technology | Purpose |
|---|---|
| Kotlin Coroutines | All async operations — API calls, DB queries, condition evaluation |
| Kotlin Flow | Reactive state from Room to UI (`Flow<List<Reminder>>`) |

All `getCondition()` calls are `suspend` functions. The background evaluation loop runs inside a coroutine scope.

---

## Persistence

| Technology | Purpose |
|---|---|
| Room | Local database — stores `Reminder` entities |
| Moshi | JSON serialization — condition tree serialized as a JSON string in Room |
| Moshi Kotlin Codegen | `@JsonClass(generateAdapter = true)` for all serialized data classes |
| Moshi PolymorphicJsonAdapterFactory | Handles `Condition` sealed interface polymorphism |
| DataStore (Preferences) | Lightweight key-value storage — API keys, poll interval, user preferences |

---

## Networking

| Technology | Purpose |
|---|---|
| Ktor Client (Android engine) | HTTP calls for `ApiLeafCondition` subclasses (e.g. `WeatherCondition`) |
| Ktor Content Negotiation | JSON response parsing in Ktor |

---

## AI

| Technology | Purpose |
|---|---|
| Gemini SDK (`google.ai.client.generativeai`) | `CustomCondition` evaluation |
| Google Search Grounding | Real-time web search within Gemini calls |
| Gemini model | `gemini-2.0-flash` |

The Gemini API key is stored in `local.properties` and exposed via `BuildConfig`. See [CUSTOM_CONDITION.md](./CUSTOM_CONDITION.md) for setup.

---

## Dependency Injection

| Technology | Purpose |
|---|---|
| Hilt | DI framework — injects `Context`, `HttpClient`, `GeminiClient`, `DataStore` into condition classes, repositories, and services |

All `ApiLeafCondition` and `SystemLeafCondition` subclasses receive their dependencies via Hilt constructor injection.

---

## Background Processing

| Technology | Purpose |
|---|---|
| Android ForegroundService | Background condition evaluation loop |
| WorkManager | Restarting the service after device reboot or process kill |

---

## Gradle Dependencies

```kotlin
// build.gradle.kts (app)

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51")
    kapt("com.google.dagger:hilt-android-compiler:2.51")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Moshi
    implementation("com.squareup.moshi:moshi:1.15.1")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("com.squareup.moshi:moshi-adapters:1.15.1")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.15.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Ktor
    implementation("io.ktor:ktor-client-android:2.3.11")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.11")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.11")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // Gemini
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Google Play Services (for FusedLocationProvider)
    implementation("com.google.android.gms:play-services-location:21.3.0")
}
```

---

## local.properties (not committed to version control)

```
GEMINI_API_KEY=your_gemini_key_here
```

```kotlin
// Expose in build.gradle.kts
android {
    buildFeatures { buildConfig = true }
    defaultConfig {
        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${properties["GEMINI_API_KEY"]}\""
        )
    }
}
```
