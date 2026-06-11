plugins {
    id("weathernow.android.feature") // Compose + Hilt + Nav3 が全部入る
}

android {
    namespace = "com.doihei.weathernow.feature.weather.mvvm"
}

dependencies {
    // feature は domain と ui のみ参照する。network を直接触らない。
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
}
