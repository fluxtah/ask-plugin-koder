package com.fluxtah.askplugin.koder.model

import kotlinx.serialization.Serializable

@Serializable
    data class PackageFiles(val packageName: String, val fileTree: String)