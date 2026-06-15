// build.gradle.kts（ルート）
//
// ここは薄く保つのが原則。
// 個々の設定は convention plugin に委譲するので、
// ここではプラグインを "apply false" で宣言するだけ。
plugins {
    alias(libs.plugins.android.application)  apply false
    alias(libs.plugins.android.library)      apply false
    alias(libs.plugins.kotlin.compose)       apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt)                 apply false
    alias(libs.plugins.ksp)                  apply false
    alias(libs.plugins.detekt)               apply false
    alias(libs.plugins.ktlint)               apply false
    kotlin("jvm")
}

subprojects {
    pluginManager.withPlugin("io.gitlab.arturbosch.detekt") {
        extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
            config.from(rootProject.files("config/detekt/detekt.yml"))
            buildUponDefaultConfig = true
        }
    }
}