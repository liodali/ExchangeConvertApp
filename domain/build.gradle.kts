plugins {
    id("com.android.library")
    kotlin("android")
    id("kotlin-parcelize")
    kotlin("plugin.serialization") version "2.1.20"
    id("com.google.devtools.ksp")
}

android {
    compileSdk = 36
    namespace = "dali.hamza.exchangecurrencyapp.domain"

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

}

dependencies {

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.converter.moshi)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}