package com.fluxtah.askplugin.koder.model

sealed class ListClassesResult {
    data class Success(val classes: List<AstClassInfo>) : ListClassesResult()
    data class Error(val message: String) : ListClassesResult()
}

sealed class ListFunctionsResult {
    data class Success(val classes: List<AstFunctionInfo>) : ListFunctionsResult()
    data class Error(val message: String) : ListFunctionsResult()
}
