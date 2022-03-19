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
        google()
        maven("https://jitpack.io")
    }
}

rootProject.name = "PlottingCompose"

