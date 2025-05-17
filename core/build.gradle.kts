plugins {
    id("com.android.library")
    id("kotlin-android")
    kotlin("plugin.serialization") version "2.1.20"

}

android {
    compileSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.test.ext:junit-ktx:1.2.1")
    implementation("androidx.paging:paging-common-ktx:3.3.6")
    implementation("androidx.room:room-ktx:2.7.1")

    implementation("androidx.core:core-ktx:1.16.0")

    //ktor
    val ktorVersion = "3.0.0"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    //serialization
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    val coroutinesVersion = "1.10.1"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
    //dataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.preference:preference-ktx:1.2.1")
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.9.1")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.9.1")
    androidTestImplementation("org.mockito.kotlin:mockito-kotlin:3.2.0")
    androidTestImplementation("org.mockito:mockito-android:3.10.0")
    testImplementation("com.google.code.gson:gson:2.11.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test-jvm:$coroutinesVersion")


    implementation(project(":database"))
    implementation(project(":domain"))
}