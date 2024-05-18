plugins {
    kotlin("jvm") version "1.9.23"
    id("com.github.johnrengelman.shadow") version "7.1.0"
    id("com.github.fluxtah.ask-gradle-plugin") version "0.4.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.21"
}

group = "com.fluxtah.ask"
version = "0.2"

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.fluxtah:ask-plugin-sdk:0.7.2")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("org.gradle:gradle-tooling-api:8.4")

    implementation("org.apache.lucene:lucene-core:8.11.0")
    implementation("org.apache.lucene:lucene-analyzers-common:8.11.0")

    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.12.5")
    testImplementation("com.google.guava:guava:31.0.1-jre")
    testImplementation("com.google.jimfs:jimfs:1.3.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}