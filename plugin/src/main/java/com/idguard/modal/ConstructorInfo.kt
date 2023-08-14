package com.idguard.modal

import com.idguard.utils.parser
import com.thoughtworks.qdox.model.JavaConstructor

data class ConstructorInfo(
    val params: List<ParamInfo>
) {
    fun isSame(constructorInfo: ConstructorInfo): Boolean {
        if (params.size != constructorInfo.params.size) {
            return false
        }
        val paramsCount = params.size
        repeat(paramsCount) {
            val aParamsType = params[it].rawFullyQualifiedName
            val bParamsType = constructorInfo.params[it].rawFullyQualifiedName
            if (aParamsType != bParamsType) {
                return false
            }
        }
        return true
    }

    fun isCorresponding(javaConstructor: JavaConstructor): Boolean {
        val parserResult = javaConstructor.parser()
        return isSame(parserResult)
    }
}