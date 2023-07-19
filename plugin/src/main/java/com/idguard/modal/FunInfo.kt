package com.idguard.modal

class FunInfo {

    constructor(funContent: String) {
        analyze(funContent)
    }

    var modifier: String = ""

    var name: String = ""

    var returnType: String = ""

    var params: MutableList<String> = mutableListOf()

    private fun analyze(funContent: String) {



    }

    override fun toString(): String {
        return super.toString()
    }
}