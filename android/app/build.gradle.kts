plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val wingmanBackendUrl = providers.gradleProperty("WINGMAN_BACKEND_URL")
    .orElse("https://api.hakkaren.uk")
val wingmanDemoOnly = providers.gradleProperty("WINGMAN_DEMO_ONLY")
    .orElse("true")
val wingmanQaBuild = providers.gradleProperty("WINGMAN_QA_BUILD")
    .orElse("false")
    .map { it.equals("true", ignoreCase = true) }

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
        buildConfigField("String", "BACKEND_URL", "\"${wingmanBackendUrl.get()}\"")
        // 公開端點尚未符合 docs/api.md 時預設不擷取畫面、不傳送 OCR 文字；部署完成後以
        // -PWINGMAN_DEMO_ONLY=false -PWINGMAN_BACKEND_URL=https://... 啟用。
        buildConfigField("boolean", "DEMO_ONLY", wingmanDemoOnly.get())
    }

    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true
    }

    buildTypes {
        getByName("debug") {
            // 多個平行 Codex 工作區共用同一台模擬器時，可用獨立套件完成 QA，
            // 避免其他分支覆蓋正在驗收的 APK；一般 debug/release 套件名稱不變。
            if (wingmanQaBuild.get()) {
                applicationIdSuffix = ".integrationqa"
                versionNameSuffix = "-integration-qa"
            }
        }
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.3")
    implementation("androidx.savedstate:savedstate-ktx:1.2.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation(composeBom)
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // 手機端 OCR（中文，bundled 模型，免 Google Play Services）
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")

    debugImplementation(composeBom)
    debugImplementation("androidx.compose.ui:ui-tooling")
}
