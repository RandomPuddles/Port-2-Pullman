plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    // Add Firebase AI (com.google.firebase:firebase-ai) when ready for full implementation.
    // The GeminiEvaluator interface allows the app to provide its own implementation.
}
