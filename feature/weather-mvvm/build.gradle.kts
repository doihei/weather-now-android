plugins {
    id("weathernow.android.feature") // Compose + Hilt + Nav3 が全部入る
}

android {
    namespace = "com.doihei.weathernow.feature.weather.mvvm"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))

    // テスト依存
    testImplementation(libs.bundles.test.unit) // JUnit + MockK
    testImplementation(libs.kotlinx.coroutines.test)
}
