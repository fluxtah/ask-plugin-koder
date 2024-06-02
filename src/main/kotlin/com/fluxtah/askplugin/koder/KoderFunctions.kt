/*
 * Copyright (c) 2024 Ian Warwick
 * Released under the MIT license
 * https://opensource.org/licenses/MIT
 */

package com.fluxtah.askplugin.koder

import com.fluxtah.askplugin.koder.kotlin.KotlinFileRepository
import com.fluxtah.askplugin.koder.model.AstClassInfo
import com.fluxtah.askplugin.koder.model.AstFunctionInfo
import com.fluxtah.askplugin.koder.model.ListClassesResult
import com.fluxtah.askplugin.koder.model.ListFunctionsResult
import com.fluxtah.askplugin.koder.model.ListKotlinPackagesResult
import com.fluxtah.askplugin.koder.model.PackageFiles
import com.fluxtah.askplugin.koder.search.KotlinDeclaration
import com.fluxtah.askplugin.koder.search.KotlinDeclarationSearch
import com.fluxtah.askpluginsdk.Fun
import com.fluxtah.askpluginsdk.FunParam
import com.fluxtah.askpluginsdk.io.getCurrentWorkingDirectory
import com.fluxtah.askpluginsdk.logging.AskLogger
import com.fluxtah.askpluginsdk.logging.LogLevel
import kotlinx.serialization.Serializable
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
import org.apache.lucene.store.Directory
import org.apache.lucene.store.RAMDirectory
import org.gradle.tooling.GradleConnector
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Paths
import kotlin.math.min

class KoderFunctions(val logger: AskLogger, private val baseDir: String) {

    init {
        // Ensure the base directory exists
        File(baseDir).mkdirs()
    }

    @Fun("Search through an index of the code based on the current directory")
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

        val blockSize = 32

        IndexWriter(index, config).use { writer ->
            val currentWorkingDir = File(getCurrentWorkingDirectory())
            currentWorkingDir.walk()
                .filter { !it.name.equals("build") }
                .filter { it.isFile && !it.absolutePath.contains("/.") && includeExtensions.contains(it.extension) }
                .forEach { file ->
                    val lines = file.readLines()
                    lines.chunked(blockSize).forEachIndexed { blockIndex, block ->
                        val doc = Document()
                        val blockContent = block.joinToString("\n")
                        val relativePath = file.toRelativeString(currentWorkingDir)
                        val startLine = blockIndex * blockSize
                        doc.add(TextField("Content", blockContent, Field.Store.YES))
                        doc.add(StringField("Path", relativePath, Field.Store.YES))
                        doc.add(StringField("StartLine", startLine.toString(), Field.Store.YES))
                        writer.addDocument(doc)
                    }
                }
        }

        // Search
        DirectoryReader.open(index).use { reader ->
            val searcher = IndexSearcher(reader)
            val term = Term("Content", searchText.lowercase())
            val query: Query = FuzzyQuery(term, 2)

            val hits = searcher.search(query, 4)
            val results = hits.scoreDocs.map { scoreDoc ->
                val doc = searcher.doc(scoreDoc.doc)
                mapOf(
                    "path" to doc.get("Path"),
                    "startLineIndex" to doc.get("StartLine"),
                    "block" to doc.get("Content"),
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

    @Fun("Create a directory")
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

    @Fun("Create a file")
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


    @Fun("Create or writes to a file")
    fun writeFile(
        @FunParam("The relative path of the file")
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

    @Fun("Read all the text in a file, prefer using readFileBlock for large files")
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


    @Fun("List kotlin packages and associated files found in the files of the given directory")
    fun listKotlinPackages(
        @FunParam("The relative directory path to look in")
        directoryPath: String,
        @FunParam("A filter to filter package names by contains, will return all packages in the given relative directory if empty")
        filter: String
    ): ListKotlinPackagesResult {
        return try {
            val file = getSafeFile(directoryPath)
            logger.log(LogLevel.INFO, "fetching packages in: ${file.path}")
            val packageFiles = extractPackageNames(directoryPath)
            val results = if (filter.isEmpty()) {
                packageFiles.map { pf ->
                    val packageName = pf.packageName
                    PackageFiles(packageName, PathTreeBuilder(pf.filePaths).toTextTree())
                }
            } else {
                packageFiles.filter { it.packageName.contains(filter) }.map { pf ->
                    val packageName = pf.packageName
                    PackageFiles(packageName, PathTreeBuilder(pf.filePaths).toTextTree())
                }
            }

            if (results.isNotEmpty()) {
                return ListKotlinPackagesResult.Success(results)
            }

            return ListKotlinPackagesResult.NoResults
        } catch (e: Exception) {
            ListKotlinPackagesResult.Error(e.message ?: "An error occurred")
        }
    }

    @Fun("Read a block of lines from a file")
    fun readFileBlockByLines(
        @FunParam("The relative project path of the file")
        fileName: String,
        @FunParam("The line number to start reading from")
        startLine: Int,
        @FunParam("The number of lines to read")
        lineCount: Int
    ): String {
        return try {
            val file = getSafeFile(fileName)
            val startLineIdx = (startLine - 1).coerceAtLeast(0)
            val lines = file.useLines { it.drop(startLineIdx).take(lineCount).toList() }
            val block = lines.joinToString("\n")
            logger.log(LogLevel.INFO, "[Read File Block] ${file.name}")
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

    @Fun("Read a block of text from a file by character startOffset and endOffset")
    fun readFileBlockByChars(
        @FunParam("The relative project path of the file")
        fileName: String,
        @FunParam("The start offset of the block")
        startOffset: Int,
        @FunParam("The end offset of the block")
        endOffset: Int
    ): String {
        return try {
            val file = getSafeFile(fileName)
            val block = file.readText().substring(startOffset, endOffset)
            logger.log(LogLevel.INFO, "[Read File Block] ${file.name}")
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

    @Fun("Write a block of lines in place of a specified block of lines from startLine to lineCount, useful for replacing function blocks or similar")
    fun replaceLinesInFile(
        @FunParam("The relative project path of the file")
        fileName: String,
        @FunParam("The zero line number to start replacing from")
        startLine: Int,
        @FunParam("The number of lines to replace, may extend beyond the number of existing lines, set to zero (replace nothing) to just insert the new block at the start line")
        lineCount: Int,
        @FunParam("The replacement block of lines")
        block: String
    ): String {
        return try {
            val file = getSafeFile(fileName)
            val lines = file.readLines().toMutableList()
            val startLineIndex = (startLine - 1).coerceAtLeast(0)

            val newLines = replaceLines(lines, startLineIndex, lineCount, block)

            file.writeText(newLines.joinToString("\n"))
            logger.log(LogLevel.INFO, "[Replace Lines In File] ${file.name}")
            Json.encodeToString(mapOf("replaced" to "true"))

        } catch (e: Exception) {
            Json.encodeToString(
                mapOf(
                    "replaced" to "false",
                    "error" to e.message
                )
            )
        }
    }

    @Fun("Write over a block of lines in place of a specified block of lines from startLine to lineCount and returns the result")
    fun replaceLinesInText(
        @FunParam("The input text that contains specific lines we wish to replace")
        inputText: String,
        @FunParam("The line number to start replacing from")
        startLine: Int,
        @FunParam("The number of lines to replace, may extend beyond the number of existing lines, set to zero (replace nothing) to just insert the new block at the start line")
        lineCount: Int,
        @FunParam("The replacement block of lines")
        block: String
    ): String {
        val startLineIndex = (startLine - 1).coerceAtLeast(0)
        return try {
            logger.log(LogLevel.INFO, "[Replace Lines] start: $startLine, lines: $lineCount, block: $block")
            val lines = inputText.lines().toMutableList()
            val newLines = replaceLines(lines, startLineIndex, lineCount, block)
            Json.encodeToString(mapOf("result" to newLines.joinToString("\n")))

        } catch (e: Exception) {
            Json.encodeToString(
                mapOf(
                    "replaced" to "false",
                    "error" to e.message
                )
            )
        }
    }

    @Fun("Count the number of lines in text based file")
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

    @Fun("List files in a directory")
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

    @Fun("Builds with Gradle")
    fun execGradle(
        @FunParam("The relative project path of the project")
        projectDir: String,
        @FunParam("The Gradle tasks to execute")
        gradleTasks: String = "",
        @FunParam("Additional arguments to pass to Gradle")
        gradleArgs: String
    ): String {
        val errorOut = ByteArrayOutputStream()
        val output = ByteArrayOutputStream()
        return try {
            val projectDirectory = getSafeFile(projectDir)
            val connector = GradleConnector.newConnector().forProjectDirectory(projectDirectory)
            connector.connect().use { connection ->
                val build = connection.newBuild()
                // Add your tasks like 'clean', 'build', etc.
                build.forTasks(*gradleTasks.split(" ").toTypedArray())

                // Pass additional arguments
                build.withArguments(*(("$gradleArgs -q ").split(" ")).toTypedArray())
                build.setStandardOutput(output)
                build.setStandardError(errorOut)
                build.run() // This will execute the build with the specified arguments
            }
            Json.encodeToString(
                mapOf(
                    "success" to "true",
                    "output" to output.toString()
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

    @Fun("Executes a make target")
    fun execMake(
        @FunParam("The relative project path of the project")
        projectDir: String,
        @FunParam("The make target to execute")
        makeTarget: String
    ): String {
        val errorOut = ByteArrayOutputStream()
        return try {
            val projectDirectory = getSafeFile(projectDir)
            val process = ProcessBuilder("make", makeTarget)
                .directory(projectDirectory)
                .redirectErrorStream(true) // Redirects error stream to the output stream
                .start()

            val processOutput = process.inputStream.bufferedReader().readText()
            process.waitFor()

            if (process.exitValue() != 0) {
                logger.log(LogLevel.ERROR, "Error executing make: $processOutput")
            }

            Json.encodeToString(
                mapOf(
                    "success" to (process.exitValue() == 0).toString(),
                    "output" to processOutput
                )
            )
        } catch (e: Exception) {
            logger.log(LogLevel.ERROR, "Error executing make: ${e.cause}")
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

    @Fun("Executes a shell command")
    fun execShellCommand(
        @FunParam("The relative project path of the project")
        projectDir: String,
        @FunParam("The shell command to execute")
        shellCommand: String
    ): String {
        val errorOut = ByteArrayOutputStream()
        return try {
            val projectDirectory = getSafeFile(projectDir)
            val process = ProcessBuilder("sh", "-c", shellCommand)
                .directory(projectDirectory)
                .redirectErrorStream(true) // Redirects error stream to the output stream
                .start()

            val processOutput = process.inputStream.bufferedReader().readText() // Captures both output and error stream
            process.waitFor()
            if (process.exitValue() != 0) {
                logger.log(LogLevel.ERROR, "Error executing shell command: $processOutput")
            }

            Json.encodeToString(
                mapOf(
                    "success" to (process.exitValue() == 0).toString(),
                    "output" to processOutput
                )
            )
        } catch (e: Exception) {
            logger.log(LogLevel.ERROR, "Error executing shell command: ${e.message}")
            Json.encodeToString(
                mapOf(
                    "success" to "false",
                    "errorMessage" to e.message,
                    "cause" to e.cause?.toString(),
                    "errorOut" to errorOut.toString()
                )
            )
        }
    }


    @Fun("Find line number by regex")
    fun findLineNumberByRegex(
        @FunParam("The relative project path of the file")
        fileName: String,
        @FunParam("The regex pattern to search with")
        pattern: String
    ): String {
        return try {
            val file = getSafeFile(fileName)
            val lines = file.readLines()
            val regex = Regex(pattern)
            val lineNumber = lines.indexOfFirst { it.contains(regex) } + 1
            Json.encodeToString(
                mapOf(
                    "index" to lineNumber.toString(),
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

    @Fun("List classes in a Kotlin file via AST, returns a list of classes with character start offset and end offset in the file")
    fun listClassesInKotlinFile(
        @FunParam("The relative project path of the file")
        fileName: String,
        @FunParam("A filter to filter class names by contains, will return all classes in the given kotlin file if empty")
        filter: String
    ): ListClassesResult {
        val repository = KotlinFileRepository()

        try {
            val file = getSafeFile(fileName)
            val ktFile = repository.parseFileOnce(file.path)

            val classes = ktFile!!.declarations.filterIsInstance<KtClass>()
            val resultClasses = mutableListOf<AstClassInfo>()

            for (klass in classes) {
                if (filter.isEmpty()) {
                    resultClasses.add(
                        AstClassInfo(
                            name = klass.name ?: "",
                            start = klass.textRange.startOffset,
                            end = klass.textRange.endOffset
                        )
                    )
                } else {
                    if (klass.name?.contains(filter, true) == true) {
                        resultClasses.add(
                            AstClassInfo(
                                name = klass.name ?: "",
                                start = klass.textRange.startOffset,
                                end = klass.textRange.endOffset
                            )
                        )
                    }
                }
            }

            return ListClassesResult.Success(resultClasses)
        } catch (e: Exception) {
            return ListClassesResult.Error(e.message ?: "An error occurred")
        }
    }

    @Fun("List functions in a Kotlin file via AST, returns a list of functions with character start offset and end offset in the file")
    fun listFunctionsInKotlinFile(
        @FunParam("The relative project path of the file")
        fileName: String,
        @FunParam("The exact class name to search for or leave empty to list top-level functions")
        className: String = "",
        @FunParam("A filter to filter function names by contains, will return all functions in the given class if empty")
        filter: String
    ): ListFunctionsResult {
        val repository = KotlinFileRepository()

        try {
            val file = getSafeFile(fileName)
            val ktFile = repository.parseFileOnce(file.path)

            val classes = ktFile!!.declarations.filterIsInstance<KtClass>()
            val resultFunctions = mutableListOf<AstFunctionInfo>()

            val declarations = if (className.isEmpty()) {
                ktFile.declarations
            } else {
                val klass = classes.find { it.name == className }
                klass?.declarations ?: emptyList()
            }

            val functions = declarations.filterIsInstance<KtNamedFunction>()

            for (function in functions) {
                if (filter.isEmpty()) {
                    resultFunctions.add(
                        AstFunctionInfo(
                            name = function.name ?: "",
                            start = function.textRange.startOffset,
                            end = function.textRange.endOffset
                        )
                    )
                } else {
                    if (function.name?.contains(filter, true) == true) {
                        resultFunctions.add(
                            AstFunctionInfo(
                                name = function.name ?: "",
                                start = function.textRange.startOffset,
                                end = function.textRange.endOffset
                            )
                        )
                    }
                }
            }

            return ListFunctionsResult.Success(resultFunctions)
        } catch (e: Exception) {
            return ListFunctionsResult.Error(e.message ?: "An error occurred")
        }
    }

    @Fun("Search for kotlin declarations in an index including classes, functions, and properties by fuzzy name, returns a list of declarations and there start and end offsets in the file making an efficient way to access code blocks")
    fun searchKotlinDeclarations(
        @FunParam("The text to search the index with, this is a fuzzy search on declaration names")
        searchText: String
    ): SearchKotlinDeclarationsResult {
        return SearchKotlinDeclarationsResult(KotlinDeclarationSearch().search(searchText))
    }

    @Serializable
    data class SearchKotlinDeclarationsResult(val results: List<KotlinDeclaration>)
}

fun replaceLines(
    lines: MutableList<String>,
    startLine: Int,
    lineCount: Int,
    block: String
): List<String> {
    val blockLines = block.lines()
    if (startLine < 1) {
        throw IllegalArgumentException("Start line must be greater than 0")
    }
    val startLineIndex = (startLine - 1)
    val endLineIndex = startLineIndex + lineCount - 1 // Corrected to ensure it's inclusive

    lines.subList(startLineIndex, min(endLineIndex + 1, lines.size)).clear()
    lines.addAll(startLineIndex, blockLines)
    return lines
}
