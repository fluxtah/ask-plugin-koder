/*
 * Copyright (c) 2024 Ian Warwick
 * Released under the MIT license
 * https://opensource.org/licenses/MIT
 */

package com.fluxtah.askplugin.koder

import com.fluxtah.askpluginsdk.Fun
import com.fluxtah.askpluginsdk.FunParam
import com.fluxtah.askpluginsdk.io.getCurrentWorkingDirectory
import com.fluxtah.askpluginsdk.logging.AskLogger
import com.fluxtah.askpluginsdk.logging.LogLevel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.search.FuzzyQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import org.apache.lucene.store.Directory
import org.apache.lucene.store.RAMDirectory
import org.gradle.tooling.GradleConnector
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Paths
import kotlin.math.min

class KoderFunctions(val logger: AskLogger, private val baseDir: String) {

    init {
        // Ensure the base directory exists
        File(baseDir).mkdirs()
    }

    @Fun("Searches for indexed code in a software project in the current directory")
    fun searchIndexedCode(
        @FunParam("The text to search the index with")
        searchText: String
    ): String {
        val analyzer = StandardAnalyzer()
        val index: Directory = RAMDirectory()
        val config = IndexWriterConfig(analyzer)
        val includeExtensions = setOf(
            "java", "kt", "js", "html", "css",
            "txt", "kts", "gradle", "c", "cpp", "h", "hpp",
            "py", "rb", "php", "sh", "json", "xml", "yml", "yaml",
            "md", "sql", "groovy", "swift", "go", "dart", "ts", "tsx",
            "cs", "fs", "fsx", "fsi", "rs", "rs.in", "rslib", "rslib.in",
            "clj", "cljs", "cljc", "edn", "rkt", "scm", "ss", "sld", "sch"
        )

        IndexWriter(index, config).use { writer ->
            val currentWorkingDir = File(getCurrentWorkingDirectory())
            currentWorkingDir.walk()
                .filter { it.isFile && !it.absolutePath.contains("/.") && includeExtensions.contains(it.extension) }
                .forEach { file ->
                    logger.log(LogLevel.DEBUG, "Indexing file: ${file.path}")
                    val doc = Document()
                    // Calculate relative path from basePath
                    val relativePath = file.toRelativeString(currentWorkingDir)
                    logger.log(LogLevel.DEBUG, "Relative path: $relativePath")
                    doc.add(TextField("Content", file.readText(), Field.Store.YES))
                    doc.add(StringField("path", relativePath, Field.Store.YES))  // Store the relative path
                    writer.addDocument(doc)
                }
        }

        // Search
        DirectoryReader.open(index).use { reader ->
            val searcher = IndexSearcher(reader)
            // Example: 'searchText' will be matched fuzzily
            val term = Term("Content", searchText.lowercase())  // Lowercase to match analyzer behavior
            val query: Query = FuzzyQuery(term, 2)  // '2' is the maximum edit distance allowed

            val hits = searcher.search(query, 10)  // search for the top 10 results

            val results = hits.scoreDocs.map { scoreDoc ->
                val doc = searcher.doc(scoreDoc.doc)
                mapOf(
                    "path" to doc.get("path"),  // Include the path in the result
                    "content" to doc.get("Content")
                )
            }

            if (results.isNotEmpty()) {
                return Json.encodeToString(
                    mapOf(
                        "results" to results
                    )
                )
            }
        }

        return Json.encodeToString(
            mapOf(
                "created" to "false",
                "result" to "No results found"
            )
        )
    }

    private fun getSafeFile(path: String): File {
        val normalizedPath = Paths.get(baseDir, path).normalize().toFile()
        if (!normalizedPath.absolutePath.startsWith(baseDir)) {
            throw SecurityException("Attempt to access outside of the base directory")
        }
        return normalizedPath
    }

    @Fun("Creates a directory for a software project")
    fun createDirectory(
        @FunParam("The desired relative path and name of the project if it does not exist already")
        directoryName: String
    ): String {
        return try {
            val directory = getSafeFile(directoryName)
            if (directory.mkdirs()) {
                logger.log(LogLevel.INFO, "Creating directory: ${directory.path}")
                Json.encodeToString(
                    mapOf(
                        "created" to "true",
                    )
                )
            } else {
                Json.encodeToString(
                    mapOf(
                        "created" to "false",
                        "error" to "Directory already exists or cannot be created"
                    )
                )
            }
        } catch (e: Exception) {
            Json.encodeToString(
                mapOf(
                    "created" to "false",
                    "error" to e.message
                )
            )
        }
    }

    @Fun("Creates a file for a software project")
    fun createFile(
        @FunParam("The desired relative path of the file")
        fileName: String
    ): String {
        return try {
            val file = getSafeFile(fileName)
            if (file.createNewFile()) {
                logger.log(LogLevel.INFO, "Creating file: ${file.path}")
                Json.encodeToString(
                    mapOf(
                        "created" to "true",
                    )
                )
            } else {
                Json.encodeToString(
                    mapOf(
                        "created" to "false",
                        "error" to "File already exists or cannot be created"
                    )
                )
            }
        } catch (e: Exception) {
            Json.encodeToString(
                mapOf(
                    "created" to "false",
                    "error" to e.message
                )
            )
        }
    }


    @Fun("Creates or writes to a file for a software project")
    fun writeFile(
        @FunParam("The relative project path of the file")
        fileName: String,
        @FunParam("The contents to write to the file")
        fileContents: String
    ): String {
        return try {
            val file = getSafeFile(fileName)
            file.writeText(fileContents)
            logger.log(LogLevel.INFO, "Writing to file: ${file.path}")
            Json.encodeToString(
                mapOf(
                    "written" to "true",
                )
            )
        } catch (e: Exception) {
            Json.encodeToString(
                mapOf(
                    "written" to "false",
                    "error" to e.message
                )
            )
        }
    }

    @Fun("Reads a file for a software project")
    fun readFile(
        @FunParam("The relative project path of the file")
        fileName: String
    ): String {
        return try {
            val file = getSafeFile(fileName)
            logger.log(LogLevel.INFO, "Reading file: ${file.path}")
            file.readText()
        } catch (e: Exception) {
            Json.encodeToString(
                mapOf(
                    "read" to "false",
                    "error" to e.message
                )
            )
        }
    }

    @Fun("List kotlin packages in a given folder directory")
    fun listKotlinPackages(
        @FunParam("The relative directory path to look in")
        directoryPath: String,
        @FunParam("A filter to filter package names by contains, will return all packages in the given relative directory if empty")
        filter: String
    ): String {
        return try {
            val file = getSafeFile(directoryPath)
            logger.log(LogLevel.INFO, "fetching packages in: ${file.path}")
            val packageFiles = extractPackageNames(directoryPath)
            val results = packageFiles.filter { it.packageName.contains(filter)  }.map { pf ->
                val packageName = pf.packageName
                mapOf(
                    "packageName" to packageName,
                    "inFiles" to pf.filePaths.joinToString(", ")
                )
            }

            if (results.isNotEmpty()) {
                return Json.encodeToString(
                    mapOf(
                        "results" to results
                    )
                )
            }

            return Json.encodeToString(
                mapOf(
                    "created" to "false",
                    "result" to "No results found"
                )
            )
        } catch (e: Exception) {
            Json.encodeToString(
                mapOf(
                    "read" to "false",
                    "error" to e.message
                )
            )
        }
    }

    @Fun("Reads a block of lines from a file for a software project")
    fun readFileBlock(
        @FunParam("The relative project path of the file")
        fileName: String,
        @FunParam("The line number to start reading from")
        startLine: Int,
        @FunParam("The number of lines to read")
        lineCount: Int
    ): String {
        return try {
            val file = getSafeFile(fileName)
            val lines = file.useLines { it.drop(startLine - 1).take(lineCount).toList() }
            val block = lines.joinToString("\n")
            logger.log(LogLevel.INFO, "[Read File Block] $block")
            block
        } catch (e: Exception) {
            Json.encodeToString(
                mapOf(
                    "read" to "false",
                    "error" to e.message
                )
            )
        }
    }

    @Fun("Writes a block of lines in place of a specified block of lines from startLine to lineCount")
    fun replaceLinesInFile(
        @FunParam("The relative project path of the file")
        fileName: String,
        @FunParam("The zero indexed line number to start writing from")
        startLine: Int,
        @FunParam("The number of lines to write over, may extend beyond the existing line length")
        lineCount: Int,
        @FunParam("The block of lines to write in place")
        block: String
    ): String {
        return try {
            val file = getSafeFile(fileName)
            val lines = file.readLines().toMutableList()

            val newLines = replaceLines(lines, startLine, lineCount, block)

            file.writeText(newLines.joinToString("\n"))
            logger.log(LogLevel.INFO, "[Write File Block] ${file.name} - $block")
            Json.encodeToString(mapOf("written" to "true"))

        } catch (e: Exception) {
            Json.encodeToString(
                mapOf(
                    "written" to "false",
                    "error" to e.message
                )
            )
        }
    }

    @Fun("Writes over a block of lines in place of a specified block of lines from startLine to lineCount and returns the result")
    fun replaceLinesInText(
        @FunParam("The input text that contains specific lines we wish to replace")
        inputText: String,
        @FunParam("The zero indexed line number to start writing from")
        startLine: Int,
        @FunParam("The number of lines to write over, may extend beyond the existing line length")
        lineCount: Int,
        @FunParam("The block of lines to write in place")
        block: String
    ): String {
        return try {
            logger.log(LogLevel.INFO, "[Replace Lines] start: $startLine, lines: $lineCount, block: $block")
            val lines = inputText.lines().toMutableList()
            val newLines = replaceLines(lines, startLine, lineCount, block)
            Json.encodeToString(mapOf("result" to newLines.joinToString("\n")))

        } catch (e: Exception) {
            Json.encodeToString(
                mapOf(
                    "written" to "false",
                    "error" to e.message
                )
            )
        }
    }

    @Fun("Counts the number of lines in a file for a software project")
    fun countLinesInFile(
        @FunParam("The relative project path of the file")
        fileName: String
    ): String {
        return try {
            val file = getSafeFile(fileName)
            val lineCount = file.useLines { lines -> lines.count() }
            Json.encodeToString(
                mapOf(
                    "lineCount" to lineCount.toString(),
                )
            )
        } catch (e: Exception) {
            Json.encodeToString(
                mapOf(
                    "error" to e.message
                )
            )
        }
    }

    @Fun("Lists files in a directory for a software project")
    fun listFilesInDirectory(
        @FunParam("The relative project path of the directory")
        directoryName: String
    ): String {
        return try {
            val directory = getSafeFile(directoryName)
            val fileList = listFilesInImmediateSubdirectories(directory)
                .map { it.replace("$baseDir/", "") }
            Json.encodeToString(
                mapOf(
                    "files" to fileList,
                )
            )
        } catch (e: Exception) {
            Json.encodeToString(
                mapOf(
                    "error" to e.message
                )
            )
        }
    }

    private fun listFilesInImmediateSubdirectories(file: File): List<String> {
        val fileList = mutableListOf<String>()
        if (file.isDirectory) {
            file.listFiles()?.forEach { subfile ->
                fileList.add(subfile.absolutePath)
                if (subfile.isDirectory) {
                    subfile.listFiles()?.forEach { innerFile ->
                        if (innerFile.isFile || innerFile.isDirectory) {
                            fileList.add(innerFile.absolutePath)
                        }
                    }
                }
            }
        }
        return fileList
    }

    @Fun("Replaces specific text in a file for a software project")
    fun replaceTextInFile(
        @FunParam("The relative project path of the file")
        fileName: String,
        @FunParam("The text to replace")
        textToReplace: String,
        @FunParam("The replacement text")
        replacementText: String
    ): String {
        return try {
            val file = getSafeFile(fileName)
            val fileText = file.readText()

            if (fileText.contains(textToReplace)) {
                val newText = fileText.replace(textToReplace, replacementText)
                file.writeText(newText)
                logger.log(LogLevel.INFO, "Replaced specific text in file: ${file.path}")
                Json.encodeToString(
                    mapOf(
                        "replaced" to "true",
                    )
                )
            } else {
                Json.encodeToString(
                    mapOf(
                        "replaced" to "false",
                        "error" to "Text to replace not found"
                    )
                )
            }
        } catch (e: Exception) {
            Json.encodeToString(
                mapOf(
                    "replaced" to "false",
                    "error" to e.message
                )
            )
        }
    }

    @Fun("Replaces text in a file by character index for a software project")
    fun replaceTextInFileByIndex(
        @FunParam("The relative project path of the file")
        fileName: String,
        @FunParam("The start index of the text to replace")
        startIndex: Int,
        @FunParam("The end index of the text to replace")
        endIndex: Int,
        @FunParam("The replacement text")
        replacementText: String
    ): String {
        return try {
            val file = getSafeFile(fileName)
            val fileText = file.readText()

            if (startIndex in 0 until endIndex && endIndex <= fileText.length) {
                val newText = StringBuilder(fileText)
                    .replace(startIndex, endIndex, replacementText)
                    .toString()
                file.writeText(newText)
                logger.log(LogLevel.INFO, "Replaced content in file: ${file.path}")
                Json.encodeToString(
                    mapOf(
                        "replaced" to "true",
                    )
                )
            } else {
                Json.encodeToString(
                    mapOf(
                        "replaced" to "false",
                        "error" to "Invalid start or end index"
                    )
                )
            }
        } catch (e: Exception) {
            Json.encodeToString(
                mapOf(
                    "replaced" to "false",
                    "error" to e.message
                )
            )
        }
    }

    @Fun("Builds a software project with Gradle")
    fun execGradle(
        @FunParam("The relative project path of the project")
        projectDir: String,
        @FunParam("The Gradle tasks to execute")
        gradleTasks: String = "",
        @FunParam("Additional arguments to pass to Gradle")
        gradleArgs: String
    ): String {
        val errorOut = ByteArrayOutputStream()
        return try {
            val projectDirectory = getSafeFile(projectDir)
            val connector = GradleConnector.newConnector().forProjectDirectory(projectDirectory)
            connector.connect().use { connection ->
                val build = connection.newBuild()
                // Add your tasks like 'clean', 'build', etc.
                build.forTasks(*gradleTasks.split(" ").toTypedArray())

                // Pass additional arguments
                build.withArguments(*gradleArgs.split(" ").toTypedArray())
                build.setStandardOutput(System.out)
                build.setStandardError(errorOut)
                build.run() // This will execute the build with the specified arguments
            }
            Json.encodeToString(
                mapOf(
                    "success" to "true",
                )
            )
        } catch (e: Exception) {
            logger.log(LogLevel.ERROR, "Error executing Gradle: ${e.cause}")
            Json.encodeToString(
                mapOf(
                    "success" to "false",
                    "errorMessage" to e.message,
                    "cause" to e.cause.toString(),
                    "errorOut" to errorOut.toString()
                )
            )
        }
    }
}

fun replaceLines(
    lines: MutableList<String>,
    startLine: Int,
    lineCount: Int,
    block: String
): List<String> {
    val blockLines = block.lines()
    val endLine = startLine + lineCount - 1 // Corrected to ensure it's inclusive

    lines.subList(startLine, min(endLine + 1, lines.size))
        .clear()
    lines.addAll(startLine, blockLines)
    return lines
}
