plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "pw.x4.autovpn"
    compileSdk = 35

    defaultConfig {
        applicationId = "pw.x4.autovpn"
        minSdk = 26          // Android 8.0 — UsageStatsManager и adaptive-иконки доступны
        targetSdk = 35       // Android 15
        versionCode = 1
        versionName = "0.1.0"
    }

    // Подпись release-сборки ключом из GH Secrets (env). Стабильный ключ обязателен,
    // иначе OTA-обновление не встанет поверх (несовпадение подписи). Локально, без env,
    // signingConfig просто не создаётся — debug собирается как обычно.
    val keystoreB64 = System.getenv("ANDROID_KEYSTORE_BASE64")
    signingConfigs {
        if (!keystoreB64.isNullOrBlank()) {
            create("release") {
                val ksFile = java.io.File(System.getProperty("java.io.tmpdir"), "autovpn-release.jks")
                ksFile.writeBytes(java.util.Base64.getDecoder().decode(keystoreB64))
                storeFile = ksFile
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    // Room — список триггер-приложений (чёрный список)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore — флаги автоматизации и выбранный VPN
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.kotlinx.coroutines.android)
}
