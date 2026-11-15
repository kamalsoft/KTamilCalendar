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

// The static site (app files + generated month data) that GitHub Pages serves: web/build/site.
val siteDir = layout.buildDirectory.dir("site")

val copyAppFiles by tasks.registering(Copy::class) {
    from("src/main/resources/web")
    into(siteDir)
}

val generateMonthData by tasks.registering(JavaExec::class) {
    dependsOn(copyAppFiles)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.kamalsoft.tamilcalendar.tv.web.SiteKt")
    args(siteDir.get().asFile.absolutePath)
}

tasks.register("site") {
    description = "Builds the GitHub Pages site into web/build/site."
    group = "build"
    dependsOn(generateMonthData)
}
