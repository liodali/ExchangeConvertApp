import java.util.Calendar
import java.util.Properties


plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply true
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
    compileSdk = 34
    namespace = "dali.hamza.echangecurrencyapp"
    defaultConfig {
        applicationId = "dali.hamza.exchangecurrencyapp"
        minSdk = 26
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            applicationIdSuffix = ".Build${Calendar.getInstance().time.time}"
            resValue("string", "token", properties.getProperty("token", ""))
        }
        debug {
            resValue("string", "token", properties.getProperty("token", ""))
            applicationIdSuffix = ".debug"

        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = composeVersion
        kotlinCompilerVersion = kotlinVersion
    }
}

dependencies {

    val room_version = "2.6.1"
    val lifecycle_version = "2.8.5"

    val paging_version = "3.3.2"

    val coroutines_version = "1.4.3"
    val workerManager_version = "2.9.1"
    val nav_version = "2.8.0"
    val koin_android_version = "4.0.0-RC1"
    val composeBom = platform("androidx.compose:compose-bom:2024.09.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.5")

    // compose
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    // such as input and measurement/layout
    implementation("androidx.compose.ui:ui")

    // Android Studio Preview support
    implementation("androidx.compose.ui:ui-tooling-preview")

    // the icons but not the material library (e.g. when using Material3 or a
    // custom design system based on Foundation)
    implementation("androidx.compose.material:material-icons-core")
    // Optional - Add full set of material icons
    implementation("androidx.compose.material:material-icons-extended")
    // Optional - Add window size utils
    implementation("androidx.compose.material3:material3-window-size-class")

    debugImplementation("androidx.compose.ui:ui-tooling")


    //coil
    // implementation "com.google.accompanist:accompanist-coil:0.34.0"
    implementation("io.coil-kt:coil-compose:2.6.0")//io.coil-kt.coil3:coil:3.0.0-alpha01
    implementation("io.coil-kt.coil3:coil-compose-core:3.0.0-alpha07")
//io.coil-kt.coil3:coil:3.0.0-alpha01


    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines_version")


    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-service:$lifecycle_version")

    // alternatively - without Android dependencies for tests
    testImplementation("androidx.paging:paging-common-ktx:$paging_version")
    //paging
    implementation("androidx.paging:paging-runtime-ktx:$paging_version")

    // optional - Jetpack Compose integration
    implementation("androidx.paging:paging-compose:$paging_version")
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    //picasso
    implementation("com.squareup.picasso:picasso:2.71828")

    //room
    implementation("androidx.room:room-runtime:$room_version")
    // optional - Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")


    //fragment ktx
    implementation("androidx.fragment:fragment-ktx:1.8.3")
    // Activity KTX for viewModels()
    implementation("androidx.activity:activity-ktx:1.9.2")

    // WorkerManager dependencies
    implementation("androidx.work:work-runtime-ktx:$workerManager_version")
    //dataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.preference:preference-ktx:1.2.1")

    // Kotlin
    implementation("androidx.navigation:navigation-fragment-ktx:$nav_version")
    implementation("androidx.navigation:navigation-ui-ktx:$nav_version")


    // Feature module Support
    implementation("androidx.navigation:navigation-dynamic-features-fragment:$nav_version")

    // Testing Navigation
    androidTestImplementation("androidx.navigation:navigation-testing:$nav_version")

    // Jetpack Compose Integration
    implementation("androidx.navigation:navigation-compose:2.8.0")

    //Koin
    implementation("io.insert-koin:koin-android:$koin_android_version")
    // Jetpack WorkManager
    implementation("io.insert-koin:koin-androidx-workmanager:$koin_android_version")
    // Navigation Graph
    implementation("io.insert-koin:koin-androidx-navigation:$koin_android_version")
    // jetpack compose
    implementation("io.insert-koin:koin-androidx-compose:$koin_android_version")

    implementation(project(":database"))
    implementation(project(":core"))
    implementation(project(":domain"))

    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("androidx.preference:preference-ktx:1.2.1")
    testImplementation("androidx.datastore:datastore-core:1.1.1")
    testImplementation("androidx.datastore:datastore-preferences:1.1.1")
    androidTestImplementation("com.android.support.test.espresso:espresso-intents:3.3.0")
    androidTestImplementation("com.android.support.test.espresso:espresso-contrib:3.3.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    testImplementation("com.squareup.okhttp3:mockwebser)ver:4.9.1")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.9.1")
    androidTestImplementation("org.mockito.kotlin:mockito-kotlin:3.2.0")
    androidTestImplementation("org.mockito:mockito-android:3.10.0")
    androidTestImplementation("com.google.code.gson:gson:2.10.1")
    // Optional -- UI testing with Compose
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$composeVersion")

    androidTestImplementation(composeBom)

}