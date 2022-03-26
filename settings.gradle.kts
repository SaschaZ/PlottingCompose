pluginManagement {
    val kotlinVersion: String by settings
    val composeVersion: String by settings
    plugins {
        id("org.jetbrains.kotlin.jvm") version kotlinVersion
        id("org.jetbrains.compose") version composeVersion
        id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
    }
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://jitpack.io")
    }
}

rootProject.name = "PlottingCompose"

