/*
 * Copyright (c) 2024 Ian Warwick
 * Released under the MIT license
 * https://opensource.org/licenses/MIT
 */

package com.fluxtah.askplugin.koder.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinKotlinDeclarationSearchTest {
    @Test
    fun searchDeclarations() {
        val search = KotlinDeclarationSearch()
        val results = search.search("AssistantsApi")

        assertEquals(1, results.size)
        assertTrue(results[0].fqPath.contains("AssistantsApi"))
        assertEquals("AssistantsApi", results[0].name)
    }
}
