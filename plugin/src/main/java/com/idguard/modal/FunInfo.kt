package com.idguard.modal

data class FunInfo(
    val modifier: List<String> = emptyList(),

    val name: String = "",

    val returnType: String = "",

    val params: List<String> = emptyList(),
)