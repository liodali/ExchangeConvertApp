import java.util.Calendar
import java.util.Properties


plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.devtools.ksp") version "2.3.6" apply true
    alias(libs.plugins.compose.compiler) apply true

}
// Properties loading
val properties = Properties().apply {
    if (rootProject.file("local.properties").exists()) {
        load(rootProject.file("local.properties").inputStream())
    }
}
val composeVersion = rootProject.extra.get("compose_version") as String
val kotlinVersion = rootProject.extra.get("kotlin_version") as String

android {
    compileSdk = 36
    namespace = "dali.hamza.echangecurrencyapp"
    defaultConfig {
        applicationId = "com.sovereignledger.app"
        minSdk = 26
        // injectable from CI (-PversionCode=… / -PversionName=…)
        versionCode = (properties.getOrDefault("versionCode", "1") as String).toInt()
        versionName = (properties.getOrDefault("versionName", "1.0.0") as String)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Release signing: keystore NEVER committed — credentials come from
    // local.properties (local) or environment variables (CI). See
    // docs/play-release.md.
    val releaseKeystorePath = providers.environmentVariable("SIGNING_KEYSTORE_PATH").orNull
        ?: properties.getOrDefault("signing.keystore.path", "").toString()
    val releaseStorePassword = providers.environmentVariable("SIGNING_STORE_PASSWORD").orNull
        ?: properties.getOrDefault("signing.store.password", "").toString()
    val releaseKeyAlias = providers.environmentVariable("SIGNING_KEY_ALIAS").orNull
        ?: properties.getOrDefault("signing.key.alias", "").toString()
    val releaseKeyPassword = providers.environmentVariable("SIGNING_KEY_PASSWORD").orNull
        ?: properties.getOrDefault("signing.key.password", "").toString()

    signingConfigs {
        if (releaseKeystorePath.isNotBlank()) {
            create("release") {
                // relative paths in .env/local.properties resolve from the repo root
                storeFile = rootProject.file(releaseKeystorePath)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            resValue("string", "token", properties.getOrDefault("token", "").toString())
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
        debug {
            resValue("string", "token", properties.getOrDefault("token", "").toString())
            applicationIdSuffix = ".debug"

        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = composeVersion
        //kotlinCompilerVersion = kotlinVersion
    }
}

dependencies {


    val composeBom = platform("androidx.compose:compose-bom:2025.04.01")
    implementation(composeBom)

    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.material)
    implementation(libs.core.splashscreen)
    implementation(libs.lifecycle.runtime.compose)

    // compose
    implementation(libs.material3)
    implementation(libs.foundation)
    // such as input and measurement/layout
    implementation(libs.ui)

    // Android Studio Preview support
    implementation(libs.ui.tooling.preview)

    // the icons but not the material library (e.g. when using Material3 or a
    // custom design system based on Foundation)
    implementation(libs.material.icons.core)
    // Optional - Add full set of material icons
    implementation(libs.material.icons.extended)
    // Optional - Add window size utils
    implementation(libs.material3.window.size.class1)

    debugImplementation(libs.ui.tooling)


    //coil
    // implementation "com.google.accompanist:accompanist-coil:0.34.0"
    implementation(libs.coil.compose)//io.coil-kt.coil3:coil:3.0.0-alpha01
    implementation(libs.coil.compose.core)
//io.coil-kt.coil3:coil:3.0.0-alpha01


    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)


    // ViewModel
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.service)

    // alternatively - without Android dependencies for tests
    testImplementation(libs.paging.common.ktx)
    //paging
    implementation(libs.paging.runtime.ktx)

    // optional - Jetpack Compose integration
    implementation(libs.paging.compose)
    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.logging.interceptor)

    //picasso
    implementation(libs.picasso)

    //room
    implementation(libs.room.runtime)
    // optional - Kotlin Extensions and Coroutines support for Room
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)


    //fragment ktx
    implementation(libs.androidx.fragment.ktx)
    // Activity KTX for viewModels()
    implementation(libs.androidx.activity.ktx)

    // WorkerManager dependencies
    implementation(libs.androidx.work.runtime.ktx)
    //dataStore
    implementation(libs.datastore.preferences)
    implementation(libs.preference.ktx)

    // Kotlin
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)


    // Feature module Support
    implementation(libs.androidx.navigation.dynamic.features.fragment)

    // Testing Navigation
    androidTestImplementation(libs.androidx.navigation.testing)

    // Jetpack Compose Integration
    implementation(libs.androidx.navigation.compose)

    //Koin
    implementation(libs.koin.android)
    // Jetpack WorkManager
    implementation(libs.koin.androidx.workmanager)
    // Navigation Graph
    implementation(libs.koin.androidx.navigation)
    // jetpack compose
    implementation(libs.koin.androidx.compose)


    implementation(libs.ktor.client.core)


    implementation(project(":database"))
    implementation(project(":shared"))
    implementation(project(":core"))
    implementation(project(":domain"))

    testImplementation(libs.androidx.core.ktx)
    testImplementation(libs.preference.ktx)
    testImplementation(libs.androidx.datastore.core)
    testImplementation(libs.datastore.preferences)
    androidTestImplementation(libs.espresso.intents)
    androidTestImplementation(libs.espresso.contrib)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    testImplementation(libs.mockwebserver)
    androidTestImplementation(libs.mockwebserver)
    androidTestImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.gson)
    // Optional -- UI testing with Compose
    androidTestImplementation(libs.androidx.ui.test.junit4.android)
    debugImplementation(libs.androidx.ui.test.manifest)

    androidTestImplementation(composeBom)

}