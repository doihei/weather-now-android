pluginManagement {
    // build-logic（convention plugin）を先にビルドするための宣言
    // iOS に相当するものはない。Android 固有の仕組み。
    includeBuild("build-logic")

    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        kotlin("jvm") version "2.4.0"
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "WeatherNow"
// ─── モジュール宣言 ───────────────────────────────────────────────────────────
// iOS SPM の Package.swift で path: を明示したのに対応。
// ここで include() したパスが自動的にモジュールとして認識される。

include(":app")

// core 層（iOS の CoreModels / CoreNetwork / WeatherDomain / CoreUI に対応）
include(":core:model")
include(":core:network")
include(":core:domain")
include(":core:ui")

// feature 層（iOS の WeatherFeatureMVVM / WeatherFeatureTCA に対応）
include(":feature:weather-mvvm")
include(":feature:weather-mvi")
 