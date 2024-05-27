/*
 * Copyright (c) 2024 Ian Warwick
 * Released under the MIT license
 * https://opensource.org/licenses/MIT
 */

package com.fluxtah.askplugin.koder

import com.fluxtah.askplugin.koder.model.AstClassInfo
import com.fluxtah.askplugin.koder.model.AstFunctionInfo
import com.fluxtah.askplugin.koder.model.ListClassesResult
import com.fluxtah.askplugin.koder.model.ListFunctionsResult
import com.fluxtah.askpluginsdk.io.getCurrentWorkingDirectory
import com.fluxtah.askpluginsdk.logging.AskLogger
import io.mockk.mockk
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class KoderFunctionsTest {

    val logger: AskLogger = mockk(relaxed = true)
    val dir = getCurrentWorkingDirectory()
    val fn = KoderFunctions(logger, dir)

    @Test
    fun testWriteFileBlock() {
        val lines = """
        A
        B
        C
        D
        E
        F
        G
        H
        I
        J
        K
        """.trimIndent().lines().toMutableList()


        // Read the file again to check if it has been updated
        val updatedContent = replaceLines(lines, 2, 2, """
            H
            E
            L
            L
            O
        """.trimIndent())

        val expected = """
        A
        B
        H
        E
        L
        L
        O
        E
        F
        G
        H
        I
        J
        K
        """.trimIndent()

        println(updatedContent)

        assertEquals(expected, updatedContent.joinToString("\n"))
    }


    @Test
    fun testWriteFileBlock2() {
        val lines = """
        A
        B
        C
        D
        E
    """.trimIndent().lines().toMutableList()

        val updatedContent = replaceLines(lines, 4, 2, """
        H
        E
        L
        L
        O
    """.trimIndent()
        )

        val expected = """
        A
        B
        C
        D
        H
        E
        L
        L
        O
        """.trimIndent()

        println(updatedContent)

        assertEquals(expected, updatedContent.joinToString("\n"))
    }

    @Test
    fun testListClassesInKotlinFile() {
        val rootDir = File("src/test/resources")
        val filePath = File(rootDir, "AssistantsApi.kt").path

        val expected = ListClassesResult.Success(listOf(
            AstClassInfo("AssistantsApi", 909, 1479),
            AstClassInfo("AssistantsApiClient", 1481, 4001),
            AstClassInfo("RunsApiClient", 4003, 7163),
            AstClassInfo("ThreadsApiClient", 7165, 9551),
            AstClassInfo("MessagesApiClient", 9553, 12418)
        ))

        val result = fn.listClassesInKotlinFile(filePath, "")

        assertEquals(expected, (result as ListClassesResult.Success))
    }

    @Test
    fun testListClassesInKotlinFileWithFilter() {
        val rootDir = File("src/test/resources")
        val filePath = File(rootDir, "AssistantsApi.kt").path

        val expected = ListClassesResult.Success(listOf(
            AstClassInfo("RunsApiClient", 4003, 7163),
        ))

        val result = fn.listClassesInKotlinFile(filePath, "run")

        assertEquals(expected, (result as ListClassesResult.Success))
    }

    @Test
    fun testListFunctionsInKotlinFile() {
        val rootDir = File("src/test/resources")
        val filePath = File(rootDir, "AssistantsApi.kt").path

        val expected = ListFunctionsResult.Success(listOf(
            AstFunctionInfo("createAssistant", 1697, 2267),
            AstFunctionInfo("modifyAssistant", 2273, 2877),
            AstFunctionInfo("getAssistant", 2883, 3421),
            AstFunctionInfo("deleteAssistant", 3427, 3999),
        ))

        val result = fn.listFunctionsInKotlinFile(filePath, "AssistantsApiClient", "")

        assertEquals(expected, (result as ListFunctionsResult.Success))
    }
}