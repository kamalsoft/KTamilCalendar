plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":shared"))

    testImplementation(libs.junit)
}

// The static site that GitHub Pages serves (web/build/site). The calendar itself runs in the browser;
// school_calendars.json is the same file the Android app ships.
val siteDir = layout.buildDirectory.dir("site")

// Sync (not Copy) so a file removed from the sources is also removed from the built site.
val copyAppFiles by tasks.registering(Sync::class) {
    from("src/main/resources/web")
    from("../app/src/main/assets/school_calendars.json") { into("engine") }
    into(siteDir)
}

tasks.register("site") {
    description = "Builds the GitHub Pages site into web/build/site."
    group = "build"
    dependsOn(copyAppFiles)
}

// Reference output for tools/check_engine.mjs: the Kotlin engine's month files, used to verify the JS port.
val referenceDir = layout.buildDirectory.dir("reference")

val referenceData by tasks.registering(JavaExec::class) {
    description = "Writes the Kotlin engine's month output to web/build/reference (for the JS parity check)."
    group = "verification"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.kamalsoft.tamilcalendar.tv.web.SiteKt")
    args(referenceDir.get().asFile.absolutePath)
}
