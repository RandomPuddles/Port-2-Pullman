# Port-2-Pullman

## Description

**Conditional Alarms** — a submission for the [Crimson Code Hackathon](https://crimsoncodehackathon.com), themed **"Reinventing the Wheel"**.

We reinvented the alarm app. Traditional alarms are triggered by time alone. Our app allows alarms to be triggered by a combination of conditions — including **weather**, **location**, and **device attributes**. Each condition can be mixed and matched, and when the combined result evaluates to `true`, the alarm fires.

---

## Tech Stack

- **Kotlin**
- **Jetpack Compose**
- **Coroutines / Flow**
- **Android Studio**
- **Room**
- **Moshi**

---

## How to Run

### Option 1 — Install on a Physical Android Device (APK)

> Requires Android 14 (API 34) or higher.

1. Download the APK: [Google Drive Download](https://drive.google.com/file/d/1w4ClsE1UHZrbLu1hd4sUqciByfU8uCnU/view?usp=sharing)
2. Transfer the `.apk` file to your Android device (via USB, Google Drive, email, etc.)
3. On your device, open the file and tap **Install**
   - If prompted, enable **Install from unknown sources** under `Settings → Security`
4. Launch **Port-2-Pullman** from your app drawer

---

### Option 2 — Run on PC via Emulator (APK)

> Requires Android Studio with an emulator set up.

1. Download the APK: [Google Drive Download](https://drive.google.com/file/d/1w4ClsE1UHZrbLu1hd4sUqciByfU8uCnU/view?usp=sharing)
2. Install [Android Studio](https://developer.android.com/studio)
3. Open **Device Manager** in Android Studio and create a virtual device with **API 34+**
4. Start the emulator
5. Drag and drop the `.apk` file onto the running emulator window — it will install automatically
6. Open the app from the emulator's app drawer

---

### Option 3 — Build and Run from Source

> Requires Android Studio and Android SDK (API 34+).

1. Clone the repository:
   ```bash
   git clone https://github.com/RandomPuddles/Port-2-Pullman.git
   cd Port-2-Pullman
   ```

2. Create a `secrets.properties` file in the project root:
   ```
   GEMINI_API_KEY=your_gemini_api_key_here
   ELEVENLABS_API_KEY=your_elevenlabs_api_key_here
   ```
   > You can leave the values blank to build without AI/TTS features.

3. Connect a physical device via USB (with USB Debugging enabled) **or** start an Android emulator

4. Build and install:
   ```bash
   ./gradlew installDebug
   ```

5. Launch the app on your device or emulator

---

## Media

### Proof of Participation

<!-- Add hackathon participation proof images here -->
<!-- Example: ![Hackathon Badge](images/hackathon-badge.png) -->

### Screenshots

<!-- Add screenshots of the running app here -->
<!-- Example: ![Home Screen](images/screenshot-home.png) -->

---

## Reflection

### Activity Description

<!-- Describe the hackathon activity, timeline, and overall experience -->

### Technical Decisions

<!-- Explain key technical choices made during the project (architecture, libraries, trade-offs, etc.) -->

### Contributions

<!-- List each team member and what they contributed -->

### Quality Assessment

<!-- Evaluate the quality of the final product — what works well, what doesn't, and what could be improved -->