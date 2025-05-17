plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") //"2.1.20-1.0.32"//"2.1.20-2.0.0"
}

android {
    namespace = "mohamedali.hamza.database"
    compileSdk = 35

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
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
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    implementation(project(":domain"))
}