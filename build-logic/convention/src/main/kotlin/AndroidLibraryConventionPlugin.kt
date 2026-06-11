import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("io.gitlab.arturbosch.detekt")
                apply("org.jlleitschuh.gradle.ktlint")
            }

            val libs = extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>()
                .named("libs")

            extensions.configure<LibraryExtension> {
                compileSdk = libs.findVersion("android-compile-sdk").get().toString().toInt()

                defaultConfig {
                    minSdk = libs.findVersion("android-min-sdk").get().toString().toInt()
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }

                testOptions {
                    unitTests.all { it.useJUnitPlatform() }
                }
            }

            extensions.getByType<org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension>()
                .jvmToolchain(17)

            dependencies {
                "testImplementation"(libs.findBundle("test-unit").get())
                "testRuntimeOnly"(libs.findLibrary("junit5-engine").get())
            }
        }
    }
}
