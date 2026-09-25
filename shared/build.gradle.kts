plugins {
    kotlin("multiplatform")
    id("com.android.library")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinxSerialization)
    id("com.google.devtools.ksp")
    id("app.cash.sqldelight")
}

val sqlDelightVersion = "2.3.2"

kotlin {
    androidTarget()
    // iOS targets.
    // The iOS framework is consumed by iosApp through a local Swift package
    // (see iosApp/SharedKMP) built by the :shared:embedAndSignAppleFrameworkForXcode
    // Gradle task, which runs as an Xcode scheme pre-action.
    listOf(
        iosX64(),
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
            implementation(libs.napier)
        }



        androidMain.dependencies {
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
    compileSdk = 36

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
