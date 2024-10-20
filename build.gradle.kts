// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {


    extra.apply {
        set("compose_version", "1.5.14")
        set("hilt_version", "2.52")
        set("kotlin_version", "1.9.24")
    }
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.7.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.52")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}