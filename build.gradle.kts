import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    `maven-publish`
}

repositories {
    mavenLocal()
    maven("https://maven.zieger.dev/releases")
    google()
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("reflect"))
    implementation(compose.desktop.currentOs)

    val kotlinCoroutinesVersion: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")

    val koinVersion: String by project
    implementation("io.insert-koin:koin-core:$koinVersion")

    implementation("dev.zieger.utils:time:3.0.13")
    implementation("dev.zieger:bybitapi:1.0.3")
    implementation("dev.zieger:tablecomposable:1.0.2")

    implementation("dev.zieger.candleproxy:client:1.0.0")
    implementation("dev.zieger.exchange:dto:1.0.0")

    val kotlinSerializationVersion: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinSerializationVersion")

    val ktorVersion: String by project
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    implementation("io.ktor:ktor-client-serialization:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    val koTestVersion: String by project
    testImplementation("io.kotest:kotest-runner-junit5:$koTestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$koTestVersion")
    testImplementation("io.kotest:kotest-property:$koTestVersion")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "15"
    kotlinOptions.freeCompilerArgs = listOf(
        "-opt-in=kotlin.RequiresOptIn"
    )
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