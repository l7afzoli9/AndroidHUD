plugins {
    // Android Application plugin (applied in the app module)
    id("com.android.application") version "8.5.0" apply false
    // Kotlin Android plugin (applied in the app module)
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    // Kotlin Kapt plugin (applied in the app module)
    id("org.jetbrains.kotlin.kapt") version "2.0.0" apply false
    // Hilt plugin (applied in the app module)
    id("dagger.hilt.android.plugin") version "2.50" apply false
}

// Configure repositories for all sub‑projects
allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
