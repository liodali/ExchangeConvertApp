plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    compileSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.test.ext:junit-ktx:1.2.1")
    implementation("androidx.paging:paging-common-ktx:3.3.2")
    implementation("androidx.room:room-ktx:2.6.1")
    val coroutines_version = "1.8.1"
    implementation("androidx.core:core-ktx:1.13.1")


    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines_version")
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
    testImplementation("com.google.code.gson:gson:2.10.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test-jvm:1.8.1")


    implementation(project(":database"))
    implementation(project(":domain"))
}