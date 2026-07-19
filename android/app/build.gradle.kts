plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "uk.hakkaren.wingman"
    compileSdk = 34

    defaultConfig {
        applicationId = "uk.hakkaren.wingman"
        minSdk = 29          // MediaProjection 前景服務型別需 API 29+
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"

        // 後端位址；部署好把這裡換成公開 URL（見 backend/、docs/api.md）
        buildConfigField("String", "BACKEND_URL", "\"https://api.hakkaren.uk\"")
        // true = 完全走本地寫死範本、不碰網路（後端還沒部署時 demo 用）
        buildConfigField("boolean", "DEMO_ONLY", "true")
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
