package com.idguard.modal

import com.idguard.utils.elementEquals
import com.idguard.utils.hasOneOf
import com.idguard.utils.parser
import com.thoughtworks.qdox.model.JavaMethod

data class MethodInfo(
    val modifier: List<String> = emptyList(),

    val rawName: String = "",

    val returnType: String = "",

    val params: List<ParamInfo> = emptyList(),

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
    var needObfuscate: Int = OverrideStatusEnum.UN_CONFIRM,

    /**
     * 方法体
     */
    val methodBody: String = ""
) {

    private fun isSameParams(params1: List<ParamInfo>, params2: List<ParamInfo>): Boolean {
        if (params1.size != params2.size) {
            return false
        }
        val pSize = params1.size
        for (i in 0 until pSize) {
            val p1 = params1[i].rawFullyQualifiedName
            val p2 = params2[i].rawFullyQualifiedName
            if (p1 != p2) {
                return false
            }
        }
        return true
    }

    /**
     * 是一个签名的函数
     */
    fun isSameFun(info: MethodInfo): Boolean {
        if (this.rawName != info.rawName) {
            return false
        }
        if (this.returnType != info.returnType) {
            return false
        }
        return isSameParams(params, info.params)
    }

    fun isCorresponding(javaMethod: JavaMethod): Boolean {
        val params2 = javaMethod.parameters.map { it.parser() }
        return rawName == javaMethod.name
            && modifier.elementEquals(javaMethod.modifiers)
            && isSameParams(params, params2)
    }

    fun isStatic(): Boolean {
        return modifier.contains("static")
    }

    fun isSamePackageVisible(): Boolean {
        val detectedModifier = listOf("public", "protected") //or no modifier
        return modifier.hasOneOf(detectedModifier) { s: String, s2: String ->
            s == s2
        } || modifier.none { it.startsWith("p") }
    }

    fun isNotSamePackageVisible(): Boolean {
        return modifier.contains("public")
    }
}