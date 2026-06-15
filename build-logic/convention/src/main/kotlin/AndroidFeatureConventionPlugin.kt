import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply<AndroidLibraryConventionPlugin>()
            apply<HiltConventionPlugin>()

            with(pluginManager) {
                apply("org.jetbrains.kotlin.plugin.compose")
                apply("org.jetbrains.kotlin.plugin.serialization")
            }

            extensions.configure<LibraryExtension> {
                buildFeatures { compose = true }
            }

            val libs = extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>()
                .named("libs")

            dependencies {
                val bom = libs.findLibrary("compose-bom").get()
                "implementation"(platform(bom))

                "implementation"(libs.findBundle("compose").get())
                "implementation"(libs.findLibrary("compose-material-icons-core").get())

                "implementation"(libs.findLibrary("navigation3-ui").get())
                "implementation"(libs.findLibrary("navigation3-runtime").get())

                "implementation"(libs.findLibrary("hilt-navigation-compose").get())
                "implementation"(libs.findLibrary("lifecycle-viewmodel-compose").get())
            }
        }
    }
}
