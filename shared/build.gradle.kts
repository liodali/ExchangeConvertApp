import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("com.android.library")
    id("org.jetbrains.compose")
    alias(libs.plugins.compose.compiler)
    id("com.google.devtools.ksp")
    id("app.cash.sqldelight")
}

// Compose Multiplatform version
val composeVersion = "1.7.0" // Compose Multiplatform 1.7.0 for Kotlin 2.1.20
val sqlDelightVersion = "2.0.2"
val ktorVersion = "3.1.2"
val koinVersion = "4.0.0"
val coroutinesVersion = "1.10.2"

kotlin {
    // Android target
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "21"
            }
        }
    }
    
    // iOS targets
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    // CocoaPods configuration
    cocoapods {
        version = "1.0.0"
        summary = "Shared KMP module for ExchangeCurrencyApp"
        homepage = "https://github.com/yourusername/ExchangeConvertApp"
        ios.deploymentTarget = "15.0"
        
        framework {
            baseName = "shared"
            isStatic = true
            export("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
            export("io.ktor:ktor-client-core:$ktorVersion")
        }
        
        pod("SQLDelight") {
            version = sqlDelightVersion
            extraOpts += listOf("-compiler-option", "-fmodules")
        }
    }

    sourceSets {
        // Common main source set
        commonMain.dependencies {
            // Compose Multiplatform
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            
            // Coroutines
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
            
            // Ktor Client
            implementation("io.ktor:ktor-client-core:$ktorVersion")
            implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
            implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
            
            // Kotlinx Serialization
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            
            // SQLDelight
            implementation("app.cash.sqldelight:runtime:$sqlDelightVersion")
            implementation("app.cash.sqldelight:coroutines-extensions:$sqlDelightVersion")
            
            // Koin
            implementation("io.insert-koin:koin-core:$koinVersion")
        }
        
        androidMain.dependencies {
            // Ktor Android Engine
            implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
            implementation("io.ktor:ktor-client-logging:$ktorVersion")
            
            // SQLDelight Android Driver
            implementation("app.cash.sqldelight:androidx-driver:$sqlDelightVersion")
            
            // Room (for hybrid database approach - Android only)
            implementation("androidx.room:room-runtime:2.7.0-alpha12")
            implementation("androidx.room:room-ktx:2.7.0-alpha12")
            implementation("androidx.room:room-paging:2.7.0-alpha12")
            
            // Koin Android
            implementation("io.insert-koin:koin-android:$koinVersion")
        }
        
        iosMain.dependencies {
            // Ktor iOS Engine (Darwin)
            implementation("io.ktor:ktor-client-darwin:$ktorVersion")
            
            // SQLDelight Native Driver
            implementation("app.cash.sqldelight:native-driver:$sqlDelightVersion")
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
