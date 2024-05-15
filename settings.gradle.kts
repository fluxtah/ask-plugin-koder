pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral() // Optional, add if your plugin or other dependencies might be there
        maven { url = uri("https://jitpack.io") }
    }
}

fun includeBuildIfPresent(path: String) {
    val projectDir = file(path)
    if (projectDir.exists()) {
        includeBuild(projectDir)
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "ask-plugin-koder"
includeBuildIfPresent("../ask-plugin-sdk")

