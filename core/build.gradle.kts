plugins {
    id("com.android.library")
    id("kotlin-android")
    alias(libs.plugins.kotlinxSerialization)

}

android {
    compileSdk = 37
    namespace = "dali.hamza.exchangecurrencyapp.core"
    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
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
    implementation(libs.junit.ktx)
    implementation(libs.paging.common.ktx)
    implementation(libs.room.ktx)

    implementation(libs.core.ktx)

    //ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.logging)

    //serialization
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    //dataStore
    implementation(libs.datastore.preferences)
    implementation(libs.preference.ktx)
    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.logging.interceptor)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    testImplementation(libs.mockwebserver)
    androidTestImplementation(libs.mockwebserver)
    androidTestImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.mockito.android)
    testImplementation(libs.gson)
    androidTestImplementation(libs.kotlinx.coroutines.test)


    implementation(project(":database"))
    implementation(project(":domain"))
}