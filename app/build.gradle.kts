import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.kamalsoft.tamilcalendar.tv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kamalsoft.tamilcalendar.tv"
        minSdk = 21
        // NOTE: Google Play requires a much newer targetSdk for new apps/updates (TV included).
        // 30 is kept because it is the product requirement; raise it before publishing on Play.
        targetSdk = 30
        versionCode = 1
        versionName = "1.0.0"

        // Leave empty to run fully offline. Set to your backend (https only) to enable sync.
        buildConfigField("String", "API_BASE_URL", "\"\"")
        buildConfigField("String", "REPO_URL", "\"https://github.com/kamalsoft/KTamilCalendar\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing is read from <repo>/keystore.properties (git-ignored), never hard-coded.
    // See README "Install on your TV" for creating the keystore.
    val keystoreProps = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { f ->
        Properties().apply { f.inputStream().use { load(it) } }
    }
    signingConfigs {
        if (keystoreProps != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // java.time (LocalDate, ZonedDateTime...) only exists natively from API 26.
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // targetSdk 30 is an explicit product requirement; without this the release
        // lintVital task fails the build. Remove once targetSdk is raised for Play.
        disable += "ExpiredTargetSdkVersion"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.tv.material)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    // Brings TLS 1.3 to API 21–28 (platform TLS 1.3 only exists from Android 10).
    implementation(libs.conscrypt.android)
    // QR code for the GitHub link on the About screen (TVs can't open links).
    implementation(libs.zxing.core)

    testImplementation(libs.junit)
}

tasks.withType<Test>().configureEach {
    System.getProperty("exportPath")?.let { systemProperty("exportPath", it) }
    System.getProperty("wavPath")?.let { systemProperty("wavPath", it) }
    System.getProperty("ragaWav")?.let { systemProperty("ragaWav", it) }
}
