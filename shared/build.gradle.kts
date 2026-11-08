// Sentry KMP Gradle plugin: configures the Sentry Cocoa linker on Apple
// targets. The Cocoa SDK itself comes from the Xcode project's SwiftPM
// package (provider NONE — see sentryKmp block below); the SDK artifact
// and this plugin both ship from Maven Central / the Plugin Portal, so
// Gradle builds never depend on GitHub's release CDN.
import io.sentry.kotlin.multiplatform.gradle.AppleDependencyProvider

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.sentryKmp)
    id("com.google.devtools.ksp")
    id("app.cash.sqldelight")
}

val sqlDelightVersion = libs.versions.sqlDelightVersion.get()

// The Sentry Cocoa SDK is linked by the Xcode project (SwiftPM package,
// exact 9.29.2 — the version KMP 0.29 is built against), so the plugin
// must not install a second copy. Provider NONE = link-only.
sentryKmp {
    autoInstall.apple.provider = AppleDependencyProvider.NONE
}

kotlin {
    androidTarget()
    // iOS targets.
    // The iOS framework is consumed by iosApp through a local Swift package
    // (see iosApp/SharedKMP) built by the :shared:embedAndSignAppleFrameworkForXcode
    // Gradle task, which runs as an Xcode scheme pre-action.
    // NOTE: no iosX64 — Compose 1.12+ (Kotlin 2.4 toolchain) no longer
    // publishes Intel iOS-simulator artifacts; Apple Silicon only.
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
            binaryOption("bundleId", "dali.hamza.shared")
        }
    }

    sourceSets {
        // Common main source set
        commonMain.dependencies {
            // Compose Multiplatform
            implementation(libs.runtime)
            implementation(libs.jetbrains.foundation)
            implementation(libs.jetbrains.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            // Real icon set for the Sovereign Ledger redesign (replaces text glyphs)
            implementation(compose.materialIconsExtended)
            implementation(libs.jetbrains.navigation.compose)

            // Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // Ktor Client
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)

            implementation(libs.ktor.serialization.kotlinx.json)
            // Kotlinx Serialization
            implementation(libs.kotlinx.serialization.json)

            // SQLDelight
            implementation(libs.sqldelight.runtime)
            implementation(libs.coroutines.extensions)

            // Koin
            implementation(libs.koin.core)

            // Crash reporting — one Sentry init for both platforms
            // (sentry-android / sentry-cocoa under the hood). Cocoa SDK
            // itself is linked from the Xcode project's SwiftPM package.
            implementation(libs.sentry.kotlin.multiplatform)
            implementation(libs.napier)
        }



        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }

        androidMain.dependencies {
            // Biometric app-lock (BiometricPrompt in the platform actual)
            implementation(libs.androidx.biometric)
            // In-app language override (AppCompatDelegate per-app locales)
            implementation(libs.appcompat)
            // Local rate-alert background job (WorkManager worker actual)
            implementation(libs.androidx.work.runtime.ktx)
            // Ktor Android Engine
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.ktor.client.logging)

            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            // SQLDelight Android Driver
            implementation(libs.android.driver)

            // Room (for hybrid database approach - Android only)
            // Using stable version to avoid alpha-related risks
            implementation(libs.room.runtime)
            implementation(libs.room.ktx)
            implementation(libs.room.paging)

            // Koin Android
            implementation(libs.koin.android)
        }

        iosMain.dependencies {
            // Ktor iOS Engine (Darwin)
            api(libs.ktor.client.darwin)

            // SQLDelight Native Driver
            api(libs.native.driver)
            api(libs.ktor.client.logging)
        }
    }
}

android {
    namespace = "dali.hamza.shared"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

// SQLDelight configuration
sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("dali.hamza.shared.database")
            dialect("app.cash.sqldelight:sqlite-3-25-dialect:$sqlDelightVersion")
        }
    }
}
