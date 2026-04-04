plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") //"2.1.20-1.0.32"//"2.1.20-2.0.0"
}

android {
    namespace = "mohamedali.hamza.database"
    compileSdk = 36

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
    implementation(libs.core.ktx)
    implementation(libs.room.runtime)
    // optional - Kotlin Extensions and Coroutines support for Room
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    //paging
    implementation(libs.paging.runtime.ktx)
    implementation(libs.room.paging)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    implementation(project(":domain"))
}