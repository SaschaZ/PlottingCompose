import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    `maven-publish`
}

repositories {
    mavenLocal()
    google()
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("dev.zieger.utils:time:3.0.13")
    implementation("dev.zieger:bybitapi:1.0.1")

    val koTestVersion: String by project
    testImplementation("io.kotest:kotest-runner-junit5:$koTestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$koTestVersion")
    testImplementation("io.kotest:kotest-property:$koTestVersion")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "15"
}

val projectVersion: String by project

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "PlottingWithJetpackCompose"
            packageVersion = projectVersion
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            groupId = "dev.zieger"
            artifactId = "plottingcompose"
            version = projectVersion

            from(components["java"])
            artifact(getSourcesJar())
//            artifact(getDokkaJar())
        }
    }
    repositories {
        maven {
            name = "ziegerDevReleases"
            setUrl("https://maven.zieger.dev/releases")
            credentials {
                username = ""
                password = ""
            }
            authentication {
                withType<BasicAuthentication>()
            }
        }
    }
}

fun Project.getSourcesJar(): Any {
    if (tasks.findByName("sourcesJar") != null) return tasks["sourcesJar"]

    return tasks.register<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }.get()
}