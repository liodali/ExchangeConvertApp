plugins {
    id("com.android.library")
    id("kotlin-android")
    kotlin("plugin.serialization") version "2.1.20"

}

android {
    compileSdk = 36
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
    val ktorVersion = "3.1.2"
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.cio)

    implementation("io.ktor:ktor-client-logging:$ktorVersion")

    //serialization
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    val coroutinesVersion = "1.10.2"
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    //dataStore
    implementation(libs.datastore.preferences)
    implementation(libs.preference.ktx)
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-moshi:3.0.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.3.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:5.3.2")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:5.3.2")
    androidTestImplementation("org.mockito.kotlin:mockito-kotlin:6.3.0")
    androidTestImplementation("org.mockito:mockito-android:5.23.0")
    testImplementation("com.google.code.gson:gson:2.13.2")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test-jvm:$coroutinesVersion")


    implementation(project(":database"))
    implementation(project(":domain"))
}