package com.fluxtah.askplugin.koder

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class PathTreeBuilderTest {

    @Test
    fun testSimplePath() {
        val paths = listOf("include/joystick.h")
        val builder = PathTreeBuilder(paths)
        val output = builder.toTextTree().trim()
        val expected = """
          - include/
            - joystick.h
        """.trimIndent()
        assertEquals(expected, output)
    }

    @Test
    fun testNestedPaths() {
        val paths = listOf(
            "kotlin-sdk/src/nativeMain/kotlin/com/fluxtah/application/api/Application.kt",
            "kotlin-sdk/src/nativeMain/kotlin/com/fluxtah/application/api/interop/CCamera.kt"
        )
        val builder = PathTreeBuilder(paths)
        val output = builder.toTextTree().trim()
        val expected = """
        - kotlin-sdk/src/nativeMain/kotlin/com/fluxtah/application/api/
          - Application.kt
          - interop/
            - CCamera.kt
        """.trimIndent()
        assertEquals(expected, output)
    }

    @Test
    fun testMultipleSeparatePaths() {
        val paths = listOf(
            "src/joystick.c",
            "src/kotlin.c",
            "include/joystick.h"
        )
        val builder = PathTreeBuilder(paths)
        val output = builder.toTextTree().trim()
        val expected = """
          - src/
            - joystick.c
            - kotlin.c
          - include/
            - joystick.h
        """.trimIndent()
        assertEquals(expected, output)
    }

    @Test
    fun testComplexNestedPaths() {
        val paths = listOf(
            "kotlin-sdk/src/nativeMain/kotlin/com/fluxtah/application/api/Application.kt",
            "kotlin-sdk/src/nativeMain/kotlin/com/fluxtah/application/api/interop/CCamera.kt",
            "kotlin-sdk/src/nativeMain/kotlin/com/fluxtah/application/api/interop/CEmitter.kt",
            "kotlin-sdk/src/nativeMain/kotlin/com/fluxtah/application/api/interop/CInput.kt",
            "kotlin-sdk/src/nativeMain/kotlin/com/fluxtah/application/api/interop/CJoystick.kt",
            "kotlin-sdk/src/nativeMain/kotlin/com/fluxtah/application/api/interop/CLight.kt",
            "kotlin-sdk/src/nativeMain/kotlin/com/fluxtah/application/api/interop/CPhysics.kt",
            "kotlin-sdk/src/nativeMain/kotlin/com/fluxtah/application/api/interop/CSound.kt",
            "kotlin-sdk/src/nativeMain/kotlin/com/fluxtah/application/api/interop/CSpriteBatch.kt",
            "kotlin-sdk/src/nativeMain/kotlin/com/fluxtah/application/api/interop/CSpriteElement.kt",
            "kotlin-sdk/src/nativeMain/kotlin/com/fluxtah/application/api/interop/CSpriteSheet.kt",
            "kotlin-sdk/src/nativeMain/kotlin/com/fluxtah/application/api/interop/CTextBatch.kt",
            "kotlin-sdk/src/nativeMain/kotlin/com/fluxtah/application/api/interop/CTextElement.kt",
            "kotlin-sdk/src/nativeMain/kotlin/com/fluxtah/application/api/interop/HelloCallback.kt",
            "kotlin-sdk/src/nativeMain/kotlin/com/fluxtah/application/api/interop/VulkanContext.kt",
            "kotlin-sdk/src/nativeMain/kotlin/com/fluxtah/application/apps/shipgame/components/PlayerInputComponent.kt"
        )
        val builder = PathTreeBuilder(paths)
        val output = builder.toTextTree().trim()
        val expected = """
          - kotlin-sdk/src/nativeMain/kotlin/com/fluxtah/application/
            - api/
              - Application.kt
              - interop/
                - CCamera.kt
                - CEmitter.kt
                - CInput.kt
                - CJoystick.kt
                - CLight.kt
                - CPhysics.kt
                - CSound.kt
                - CSpriteBatch.kt
                - CSpriteElement.kt
                - CSpriteSheet.kt
                - CTextBatch.kt
                - CTextElement.kt
                - HelloCallback.kt
                - VulkanContext.kt
            - apps/shipgame/components/
              - PlayerInputComponent.kt
        """.trimIndent()
        assertEquals(expected, output)
    }
}
