package com.idguard.modal

import com.thoughtworks.qdox.model.JavaClass
import java.io.File

/**
 * 目前只针对java文件
 * interface 和 class 类
 */
data class ClazzInfo(
    /**
     * 修饰符
     */
    val modifier: List<String> = emptyList(),
    /**
     * 包名称
     */
    val packageName: String = "",
    /**
     * 原始路径
     */
    val rawPath: String = "",
    /**
     * 混淆后的路径
     */
    var obfuscatePath: String = "",
    /**
     * 原始类名称
     */
    val rawClazzName: String = "",

    /**
     * 全限定的名称
     */
    val fullyQualifiedName: String = "",
    /**
     * 混淆后的类名称
     */
    var obfuscateClazzName: String = "",
    /**
     * 参数集合
     */
    val fieldList: List<FieldInfo> = emptyList(),
    /**
     * 方法集合
     */
    val methodList: List<MethodInfo> = emptyList(),
    /**
     * 被嵌套的那个类
     */
    var parentNode: ClazzInfo? = null,

    /**
     * 所继承的类的名称
     */
    val extendFullQualifiedName: String,
    /**
     * 继承的那个节点
     */
    var extendNode: ClazzInfo? = null,
    /**
     * 实现了哪个接口的name
     */
    val implFullQualifiedName: List<String> = emptyList(),
    /**
     * 实现了哪个接口的节点
     */
    val implNodes: MutableList<ClazzInfo> = mutableListOf(),

    var isInterface: Boolean = false,

    var isEnum: Boolean = false,

    /**
     * 所属的文件
     */
    var belongFile: File? = null,
    /**
     * 引入的包 不包括正文代码中以 com.a.b.c 形式引入的
     */
    val imports: List<String> = emptyList(),

    /**
     * class 内容
     * 带有import package 内容
     */
    val bodyInfo: String,
) {
    fun getClassContent(): String {
        val sb = StringBuilder()
        sb.appendLine("package $packageName;")
        val imports = imports.map { "import $it;" }
        imports.forEach {
            sb.appendLine(it)
        }
        sb.append(bodyInfo)
        return sb.toString()
    }

    /**
     * 是对应的 javaClass
     */
    fun isCorrespondingJavaClass(javaClass: JavaClass): Boolean {
        return fullyQualifiedName == javaClass.fullyQualifiedName
    }
}