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
        // Kotlin compiler/build-tool artifacts are Maven Central only. Without
        // this filter Gradle probes Google Maven first for every
        // org.jetbrains.kotlin artifact and a network error there is fatal
        // (a 404 would fall through, a socket error does not) — this is what
        // failed Xcode Cloud builds 7/8 with "No route to host" on
        // dl.google.com while resolving :shared:kotlinCompilerClasspath.
        google {
            content { excludeGroup("org.jetbrains.kotlin") }
        }
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
