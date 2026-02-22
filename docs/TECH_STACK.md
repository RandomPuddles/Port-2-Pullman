# Tech Stack

## Language & IDE

| Technology | Details |
|---|---|
| Kotlin | Latest stable |
| Android Studio | Hedgehog or newer |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |

---

## Libraries

| Category | Technology | Purpose |
|---|---|---|
| UI | Jetpack Compose BOM | All UI |
| UI | Material 3 | Design system |
| UI | Compose Navigation | Screen routing |
| Async | Kotlin Coroutines | All async work |
| Async | Kotlin Flow | Reactive DB → UI |
| Database | Room | Reminder persistence |
| Preferences | DataStore | API key, settings |
| AI | Gemini SDK | Condition evaluation + web search |
| DI | Hilt | Dependency injection |
| Background | WorkManager | Durable scheduled evaluation |
| Notifications | NotificationCompat | Alarm notifications |

---

## Gradle Dependencies

```kotlin
// build.gradle.kts (app)
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51")
    ksp("com.google.dagger:hilt-android-compiler:2.51")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // Gemini
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Notifications
    implementation("androidx.core:core-ktx:1.13.1")

    // Timber (logging)
    implementation("com.jakewharton.timber:timber:5.0.1")
}
```

---

## Gradle Plugins (project level)

```kotlin
// build.gradle.kts (project)
plugins {
    id("com.android.application") version "8.4.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.dagger.hilt.android") version "2.51" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}
```

---

## local.properties

```
GEMINI_API_KEY=your_gemini_key_here
```

## BuildConfig Setup

```kotlin
// build.gradle.kts (app) — inside android block
buildFeatures {
    buildConfig = true
    compose = true
}

defaultConfig {
    buildConfigField(
        "String",
        "GEMINI_API_KEY",
        "\"${properties["GEMINI_API_KEY"]}\""
    )
}

composeOptions {
    kotlinCompilerExtensionVersion = "1.5.14"
}
```

---

## Note on kapt vs ksp

Use **KSP** (`ksp()`) instead of `kapt()` for all annotation processors (Hilt, Room). KSP is faster and is the current recommended approach for new projects.
