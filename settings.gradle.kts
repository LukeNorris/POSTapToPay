pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        // Tell Settings which AGP version to use when you apply the plugin in module builds
        id("com.android.application") version "4.1.0" apply false
        id("com.android.library")     version "4.1.0" apply false
        // Keep your Kotlin plugin versions here too
        id("org.jetbrains.kotlin.android")           version "1.8.0" apply false
        id("org.jetbrains.kotlin.plugin.serialization") version "1.8.0" apply false
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "POS TapToPay"
include(":app")
 