pluginManagement {
    val kotlinVersion: String by settings
    val composeVersion: String by settings
    plugins {
        id("org.jetbrains.kotlin.jvm") version kotlinVersion
        id("org.jetbrains.compose") version composeVersion
    }
    repositories {
        gradlePluginPortal()
        mavenCentral()
        jcenter()
        google()
        maven("https://jitpack.io")

        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://dl.bintray.com/kotlin/kotlin-dev")
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://plugins.gradle.org/m2/")
    }
}

rootProject.name = "PlottingCompose"

