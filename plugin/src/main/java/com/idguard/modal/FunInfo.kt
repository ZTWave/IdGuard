package com.idguard.modal

data class FunInfo(
    val modifier: List<String> = emptyList(),

    val name: String = "",

    val returnType: String = "",

    val params: List<String> = emptyList(),

    /**
     * 是否是重写方法
     */
    val isOverride: Boolean = false,
    /**
     * 混淆过后的名称
     */
    var obfuscateName: String = ""
)