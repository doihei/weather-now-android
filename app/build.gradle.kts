plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.doihei.weathernow"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.doihei.weathernow"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // feature モジュール（画面の実体）
    implementation(project(":feature:weather-mvvm"))
    implementation(project(":feature:weather-mvi"))
    // core モジュール
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    // Compose / Navigation 3
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.navigation3.ui)
    implementation(libs.navigation3.runtime)
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    // Lifecycle
    implementation(libs.lifecycle.runtime)
    implementation(libs.activity.compose)
}
