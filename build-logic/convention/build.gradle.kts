plugins {
    `kotlin-dsl`
}

group = "com.doihei.weathernow.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly(libs.plugins.android.application.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version.requiredVersion}" })
    compileOnly(libs.plugins.android.library.map    { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version.requiredVersion}" })
    compileOnly(libs.plugins.kotlin.compose.map     { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version.requiredVersion}" })
    compileOnly(libs.plugins.hilt.map               { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version.requiredVersion}" })
    compileOnly(libs.plugins.ksp.map                { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version.requiredVersion}" })
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "weathernow.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "weathernow.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("hilt") {
            id = "weathernow.hilt"
            implementationClass = "HiltConventionPlugin"
        }
    }
}
