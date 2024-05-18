package com.fluxtah.askplugin.koder

import org.junit.jupiter.api.Assertions.assertEquals
import java.io.File
import kotlin.test.Test

class PackageExtractorTest {

    @Test
    fun testPackageExtraction() {
        // Given
        val resourcesDirectory = File("src/test/resources")

        // When
        val packageNames = extractPackageNames(resourcesDirectory.path)

        // Then
        assertEquals("foo.bar.bat", packageNames[0].packageName)
        assertEquals("src/test/resources/PackageTest01.kt", packageNames[0].filePaths[0])
        assertEquals("src/test/resources/PackageTest02.kt", packageNames[0].filePaths[1])
        assertEquals("my.awesome.place", packageNames[1].packageName)
        assertEquals("src/test/resources/PackageTest03.kt", packageNames[1].filePaths[0])

        // Here, you would typically use assertions to check if the extracted package names match expected values.
        // This example simply runs the function to demonstrate how to set up the test.
    }
}