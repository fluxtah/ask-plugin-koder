/*
 * Copyright (c) 2024 Ian Warwick
 * Released under the MIT license
 * https://opensource.org/licenses/MIT
 */

package com.fluxtah.askplugin.koder.search

import kotlin.test.Test

class KotlinKotlinDeclarationSearchTest {
    @Test
    fun test() {
        val search = KotlinDeclarationSearch()
        search.search("AssistantsApi")
    }
}