package com.idguard.modal

data class FieldInfo(
    val name: String = "",
    val type: String = "",
    val modifier: List<String> = emptyList(),
    var obfuscateName: String = ""
)