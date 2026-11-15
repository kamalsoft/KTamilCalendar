plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Matches the Android app's Java 17 target so the classes dex cleanly into the APK.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
