package com.idguard.modal

import com.idguard.utils.elementEquals
import com.thoughtworks.qdox.model.JavaMethod

data class MethodInfo(
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
    var obfuscateName: String = "",

    /**
     * 需要混淆的标识
     */
    var needObfuscate: Boolean = false,

    /**
     * 方法体
     */
    val methodBody: String = ""
) {

    private fun isSameParams(params1: List<String>, params2: List<String>): Boolean {
        if (params1.size != params2.size) {
            return false
        }
        val aParams = params1.map { it.split(" ")[0] }
        val bParams = params2.map { it.split(" ")[0] }
        return aParams.containsAll(bParams)
    }

    /**
     * 是一个签名的函数
     */
    fun isSameParams(info: MethodInfo): Boolean {
        if (this.name != info.name) {
            return false
        }
        if (this.returnType != info.returnType) {
            return false
        }
        return isSameParams(params, info.params)
    }

    fun isCorrespondingJavaMethod(javaMethod: JavaMethod): Boolean {
        val params2 = javaMethod.parameters.map { "${it.type} ${it.name}" }
        return name == javaMethod.name
            && modifier.elementEquals(javaMethod.modifiers)
            && isSameParams(params, params2)
    }
}