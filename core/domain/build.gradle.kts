plugins {
    id("weathernow.android.library")
    id("weathernow.hilt")
}

android {
    namespace = "com.doihei.weathernow.core.domain"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.services.location)
}
