package com.idguard.modal

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
    /**
     * 是一个签名的函数
     */
    fun isSameFun(info: MethodInfo): Boolean {
        if (this.name != info.name) {
            return false
        }
        if (this.params.size != info.params.size) {
            return false
        }
        if (this.returnType != info.returnType) {
            return false
        }

        val aParams = this.params.map { it.split(" ")[0] }
        val bParams = info.params.map { it.split(" ")[0] }

        return aParams.containsAll(bParams)
    }
}