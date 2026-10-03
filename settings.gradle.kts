pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev") // Compose Multiplatform
    }
}

rootProject.name = "EchangeCurrencyApp"
include(":app")
include(":core")
include(":domain")
include(":database")
include(":shared")
