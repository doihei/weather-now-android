plugins {
    id("weathernow.android.library")
    alias(libs.plugins.kotlin.serialization) // @Serializable アノテーション用
}

android {
    namespace = "com.doihei.weathernow.core.model"
}
