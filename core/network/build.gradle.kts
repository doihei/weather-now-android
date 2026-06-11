plugins {
    id("weathernow.android.library")
    id("weathernow.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.doihei.weathernow.core.network"
}

dependencies {
    // 依存は下向きのみ。:core:model だけ参照する。
    implementation(project(":core:model"))
    implementation(libs.bundles.network)
}
