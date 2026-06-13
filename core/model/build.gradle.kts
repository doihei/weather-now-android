plugins {
    id("weathernow.android.library")
    alias(libs.plugins.kotlin.serialization) // @Serializable アノテーション用
}

android {
    namespace = "com.doihei.weathernow.core.model"
}

dependencies {
    // テスト依存
    testImplementation(libs.bundles.test.unit) // JUnit + MockK が入っている想定
    testImplementation(libs.kotlinx.coroutines.test)
}
