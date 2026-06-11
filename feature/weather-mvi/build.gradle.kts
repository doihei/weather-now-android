plugins {
    id("weathernow.android.feature")
}

android {
    namespace = "com.doihei.weathernow.feature.weather.mvi"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
}
