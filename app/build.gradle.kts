import java.util.Calendar
import java.util.Properties


plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.devtools.ksp") version "2.3.6" apply true
    alias(libs.plugins.compose.compiler) apply true
    alias(libs.plugins.google.services) apply false
}
// Firebase Cloud Messaging (server-push rate alerts — plans/server-push-alerts.md).
// Applied only when the config file exists, so builds stay green until
// google-services.json is dropped in from the Firebase console (both packages:
// com.sovereignledger.app + .debug must be registered there).
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
// Properties loading
val properties = Properties().apply {
    if (rootProject.file("local.properties").exists()) {
        load(rootProject.file("local.properties").inputStream())
    }
}
// Runtime configuration: Gradle property → local.properties → default.
// App versions are loaded separately from the shared version.xcconfig.
fun prop(name: String, fallback: String): String =
    (project.properties[name] as? String) ?: properties.getProperty(name) ?: fallback
val composeVersion = rootProject.extra.get("compose_version") as String
val kotlinVersion = rootProject.extra.get("kotlin_version") as String

// The same file is the base configuration for the iOS app in Xcode.
val appVersionProperties = Properties().apply {
    val content = providers.fileContents(rootProject.layout.projectDirectory.file("version.xcconfig"))
        .asText.get()
    content.lineSequence().map { it.substringBefore("//") }.joinToString("\n")
        .reader().use { load(it) }
}
// Explicit -P overrides remain available for ad hoc builds; local.properties
// and git tags no longer silently replace the shared app version.
val appVersionName = providers.gradleProperty("versionName").orNull
    ?: requireNotNull(appVersionProperties.getProperty("MARKETING_VERSION"))
val appVersionCode = (providers.gradleProperty("versionCode").orNull
    ?: requireNotNull(appVersionProperties.getProperty("CURRENT_PROJECT_VERSION"))).toLongOrNull()
require(appVersionName.matches(Regex("""\d+\.\d+\.\d+"""))) {
    "App version must be X.Y.Z (version.xcconfig: MARKETING_VERSION)"
}
require(appVersionCode != null && appVersionCode in 1L..2100000000L) {
    "Build number must be 1..2100000000 (version.xcconfig: CURRENT_PROJECT_VERSION)"
}

android {
    compileSdk = 37
    namespace = "dali.hamza.echangecurrencyapp"
    defaultConfig {
        applicationId = "com.sovereignledger.app"
        minSdk = 26
        // explicit: target doesn't silently move with future compileSdk bumps
        targetSdk = 37
        versionCode = requireNotNull(appVersionCode).toInt()
        versionName = appVersionName

        // GlitchTip (Sentry-compatible OSS) DSN — gradle prop →
        // local.properties → empty (crash reporting disabled).
        // CI injects it from the GLITCHTIP_DSN secret.
        buildConfigField("String", "GLITCHTIP_DSN", "\"${prop("glitchtip.dsn", "")}\"")

        // Backend host override — gradle prop → local.properties → empty
        // (the shared module's DEFAULT_HOST wins at runtime). CI passes
        // -Pserver.host from the EXCHANGE_SERVER_HOST repo variable; when
        // the production backend gets its own domain, set that variable —
        // no code change needed.
        buildConfigField("String", "SERVER_HOST", "\"${prop("server.host", "")}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Release signing — priority: CI env vars → root key.properties (ours,
    // Studio never touches it) — see script/keystore.sh. Never committed.
    val keyProperties = Properties().apply {
        val keyFile = rootProject.file("key.properties")
        if (keyFile.exists()) {
            keyFile.inputStream().use { load(it) }
        }
    }
    val releaseKeystorePath = providers.environmentVariable("SIGNING_KEYSTORE_PATH").orNull
        ?: keyProperties.getProperty("KEYSTORE_PATH").orEmpty()
    val releaseStorePassword = providers.environmentVariable("SIGNING_STORE_PASSWORD").orNull
        ?: keyProperties.getProperty("KEYSTORE_PASSWORD").orEmpty()
    val releaseKeyAlias = providers.environmentVariable("SIGNING_KEY_ALIAS").orNull
        ?: keyProperties.getProperty("KEY_ALIAS").orEmpty()
    val releaseKeyPassword = providers.environmentVariable("SIGNING_KEY_PASSWORD").orNull
        ?: keyProperties.getProperty("KEY_PASSWORD").orEmpty()

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
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            resValue("string", "token", properties.getOrDefault("token", "").toString())
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
        debug {
            resValue("string", "token", properties.getOrDefault("token", "").toString())
            // No applicationIdSuffix: debug and release share
            // com.sovereignledger.app (one google-services.json, one Firebase
            // app entry with both SHA-1s). Debug builds are told apart by the
            // bug-badged launcher icon + "(debug)" label (app/src/debug/res).
            // Single id + different keys would block installs over the
            // production app — so debug uses the release signature when the
            // key is available (key.properties / CI SIGNING_* vars), making
            // debug↔release overwrites seamless and data-preserving. Machines
            // without the key fall back to the default debug keystore.
            signingConfigs.findByName("release")?.let { signingConfig = it }
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
        buildConfig = true
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
    // crash reporting → self-hosted GlitchTip (Sentry protocol)
    implementation(libs.sentry.android)
    // logging — Napier sink is planted by shared's AndroidAppContext; the
    // app module logs too (FCM service / token lifecycle)
    implementation(libs.napier)
    // Firebase Cloud Messaging — server-push rate alerts (Phase 2).
    // Messaging only: no analytics/crashlytics (privacy stance; GlitchTip covers crashes).
    // No-op at runtime until google-services.json is present.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    // Play In-App Updates — flexible/immediate update prompts for users
    // without Play auto-update
    implementation(libs.androidx.app.update)
    implementation(libs.androidx.app.update.ktx)
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