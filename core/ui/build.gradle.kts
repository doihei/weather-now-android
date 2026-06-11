plugins {
    id("weathernow.android.library")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.doihei.weathernow.core.ui"
    buildFeatures { compose = true }
}

dependencies {
    // CoreUI は CoreModels にしか依存しない（iOS の規約と同じ）
    implementation(project(":core:model"))
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
}
