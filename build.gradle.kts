plugins {
    kotlin("jvm") version "1.9.23"
    id("com.github.johnrengelman.shadow") version "7.1.0"
    id("com.github.fluxtah.ask-gradle-plugin") version "0.4.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.21"
}

group = "com.fluxtah"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.fluxtah:ask-plugin-sdk:0.6.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("org.gradle:gradle-tooling-api:8.4")

    implementation("org.apache.lucene:lucene-core:8.11.0")
    implementation("org.apache.lucene:lucene-analyzers-common:8.11.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}