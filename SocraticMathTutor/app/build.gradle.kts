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
val geminiKey = secret("GEMINI_API_KEY")
// Which AI backend to use: "gemini" (default) or "claude".
val aiProvider = secret("AI_PROVIDER").ifBlank { "gemini" }
// Production: point the app at your own backend (see /backend) so no AI key ships in the APK.
val backendUrl = secret("BACKEND_URL")
val appToken = secret("APP_TOKEN")
val geminiModel = secret("GEMINI_MODEL").ifBlank { "gemini-2.5-flash" }
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

        buildConfigField("String", "AI_PROVIDER", "\"$aiProvider\"")
        buildConfigField("String", "ANTHROPIC_API_KEY", "\"$anthropicKey\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
        buildConfigField("String", "BACKEND_URL", "\"$backendUrl\"")
        buildConfigField("String", "APP_TOKEN", "\"$appToken\"")
        buildConfigField("String", "GEMINI_MODEL", "\"$geminiModel\"")
        buildConfigField("String", "DESMOS_API_KEY", "\"$desmosKey\"")
        buildConfigField("String", "CLAUDE_MODEL", "\"claude-sonnet-5-5\"")
    }

    signingConfigs {
        // Only created when the release secrets are present (GitHub Actions). Nothing is committed.
        val ks = System.getenv("KEYSTORE_PATH")
        if (!ks.isNullOrBlank()) {
            create("release") {
                storeFile = file(ks)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
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

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
