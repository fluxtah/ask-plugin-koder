package com.fluxtah.askplugin.koder

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.asSequence

data class PackageFiles(val packageName: String, val filePaths: List<String>)

fun extractPackageNames(directoryPath: String): List<PackageFiles> {
    val directory = Paths.get(directoryPath)
    if (!Files.exists(directory) || !Files.isDirectory(directory)) {
        println("Directory does not exist or is not a directory")
        return emptyList()
    }

    val packagesToFiles = mutableMapOf<String, MutableList<String>>()

    Files.walk(directory)
        .asSequence()
        .filter { path -> shouldProcess(path) }
        .forEach { path ->
            if (Files.isRegularFile(path) && path.toString().endsWith(".kt")) {
                Files.lines(path).use { lines ->
                    val packageName = lines.filter { it.startsWith("package ") }
                        .map { it.removePrefix("package").trim() }
                        .findFirst()
                        .orElse(null)
                    if (packageName != null) {
                        packagesToFiles.computeIfAbsent(packageName) { mutableListOf() }.add(path.toString())
                    }
                }
            }
        }

    return packagesToFiles.map { PackageFiles(it.key, it.value) }
}

fun shouldProcess(path: Path): Boolean {
    // Skip hidden directories and the build directory
    return !path.fileName.toString().startsWith(".") && !path.fileName.toString().equals("build", ignoreCase = true) && Files.isDirectory(path).not()
}
