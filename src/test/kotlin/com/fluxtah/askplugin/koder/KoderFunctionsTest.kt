/*
 * Copyright (c) 2024 Ian Warwick
 * Released under the MIT license
 * https://opensource.org/licenses/MIT
 */

package com.fluxtah.askplugin.koder

import com.fluxtah.askpluginsdk.io.getCurrentWorkingDirectory
import com.fluxtah.askpluginsdk.logging.AskLogger
import io.mockk.mockk
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.writeText
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

}