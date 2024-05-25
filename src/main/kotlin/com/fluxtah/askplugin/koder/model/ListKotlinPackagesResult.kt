package com.fluxtah.askplugin.koder.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ListKotlinPackagesResult {
    @Serializable
    @SerialName("success")
    data class Success(val results: List<PackageFiles>) : ListKotlinPackagesResult()

    @Serializable
    @SerialName("no_results")
    data object NoResults : ListKotlinPackagesResult()

    @Serializable
    @SerialName("error")
    data class Error(val message: String) : ListKotlinPackagesResult()
}