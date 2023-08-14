package com.idguard.modal

data class ParamInfo(
    val rawFullyQualifiedName: String,
    val rawName: String,
    /**
     * 混淆后的名称
     */
    var obfuscateName: String = "",
)