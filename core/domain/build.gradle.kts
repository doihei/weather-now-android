plugins {
    id("weathernow.android.library")
    id("weathernow.hilt")
}

android {
    namespace = "com.doihei.weathernow.core.domain"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:network")) // OpenMeteoApi を使うため
    implementation(libs.gms.location) // FusedLocationProviderClient

    testImplementation(libs.bundles.test.unit)
    testImplementation(libs.kotlinx.coroutines.test)
}
