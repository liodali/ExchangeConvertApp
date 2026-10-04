// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    id("com.google.devtools.ksp") version "2.3.6" apply false
    id("app.cash.sqldelight") version "2.3.2" apply false
}
buildscript {

    extra.apply {
        set("compose_version", "1.8.0")
        set("hilt_version", "2.52")
        set("kotlin_version", "2.1.20")
    }
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.gradle)
        classpath(libs.kotlin.gradle.plugin)


        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}