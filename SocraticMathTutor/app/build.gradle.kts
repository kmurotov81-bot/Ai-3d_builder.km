import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// Keys come from local.properties (local builds) or environment variables (GitHub Actions).
fun secret(name: String): String =
    localProps.getProperty(name)?.takeIf { it.isNotBlank() }
        ?: System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: ""

val anthropicKey = secret("ANTHROPIC_API_KEY")
// Desmos documents a public demo key for TESTING ONLY (check docs at desmos.com/api).
// Replace it with your own key before publishing the app.
val desmosKey = secret("DESMOS_API_KEY").ifBlank { "dcb31709b452b1cf9dc26972add0fda6" }

android {
    namespace = "com.example.socratictutor"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.socratictutor"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "ANTHROPIC_API_KEY", "\"$anthropicKey\"")
        buildConfigField("String", "DESMOS_API_KEY", "\"$desmosKey\"")
        buildConfigField("String", "CLAUDE_MODEL", "\"claude-sonnet-5-5\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
