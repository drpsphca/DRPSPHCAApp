import org.gradle.api.tasks.Copy

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
}

val wordpressAuthConfigDir = layout.buildDirectory.dir("generated/wordpressAuth/commonMain")
fun wordpressAuthorization(apiKey: String): String {
    if (apiKey.isBlank()) return ""
    return "Bearer $apiKey"
}
val wordpressAuthConfigTokens = mapOf(
    "MAIN_API_URL" to providers.gradleProperty("WORDPRESS_API_URL")
        .getOrElse("https://drpsphca.com/wp-json/"),
    "MAIN_AUTHORIZATION" to wordpressAuthorization(
        providers.gradleProperty("WORDPRESS_API_KEY").getOrElse(""),
    ),
    "TVAL_API_URL" to providers.gradleProperty("TVAL_API_URL")
        .getOrElse("https://tval.drpsphca.com/wp-json/"),
    "TVAL_AUTHORIZATION" to wordpressAuthorization(
        providers.gradleProperty("TVAL_API_KEY").getOrElse(""),
    ),
    "APP_API_URL" to providers.gradleProperty("APPX_API_URL")
        .getOrElse("https://app.drpsphca.com/wp-json/"),
    "APP_AUTHORIZATION" to wordpressAuthorization(
        providers.gradleProperty("APPX_API_KEY").getOrElse(""),
    ),
)
val wordpressAuthConfig = tasks.register<Copy>("generateWordPressAuthConfig") {
    from("src/wordpressAuth/WordPressAuthConfig.kt.template")
    into(wordpressAuthConfigDir)
    rename { "WordPressAuthConfig.kt" }
    expand(wordpressAuthConfigTokens)
    filteringCharset = "UTF-8"
    outputs.upToDateWhen { false }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.drpsphca.shared.resources"
}

kotlin {

    // Target declarations - add or remove as needed below. These define
    // which platforms this KMP module supports.
    // See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
    android {
        namespace = "com.drpsphca.shared"
        compileSdk = 37
        minSdk = 24

        withHostTestBuilder {
        }
    }

    // For iOS targets, this is also where you should
    // configure native binary output. For more information, see:
    // https://kotlinlang.org/docs/multiplatform-build-native-binaries.html#build-xcframeworks

    // A step-by-step guide on how to include this library in an XCode
    // project can be found here:
    // https://developer.android.com/kotlin/multiplatform/migrate
    val xcfName = "sharedKit"

    iosArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    // Source set declarations.
    // Declaring a target automatically creates a source set with the same name. By default, the
    // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
    // common to share sources between related targets.
    // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
    sourceSets {
        commonMain {
            kotlin.srcDir(wordpressAuthConfig)
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.composeRuntime)
                implementation(libs.composeFoundation)
                implementation(libs.composeAnimation)
                implementation(libs.composeMaterial3)
                implementation(libs.composeMaterialIconsExtended)
                implementation(libs.composeUi)
                implementation(libs.composeResources)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.coil.compose.multiplatform)
                implementation(libs.coil.network.ktor3)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.ktor.client.okhttp)
                // Add Android-specific dependencies here. Note that this source set depends on
                // commonMain by default and will correctly pull the Android artifacts of any KMP
                // dependencies declared in commonMain.
            }
        }



        iosMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
                // Add iOS-specific dependencies here. This a source set created by Kotlin Gradle
                // Plugin (KGP) that each specific iOS target (e.g., iosX64) depends on as
                // part of KMP’s default source set hierarchy. Note that this source set depends
                // on common by default and will correctly pull the iOS artifacts of any
                // KMP dependencies declared in commonMain.
            }
        }
    }

}