package com.idguard.modal

import com.thoughtworks.qdox.model.JavaClass
import com.thoughtworks.qdox.model.JavaType
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
     * 所在包名称
     */
    val packageName: String = "",
    /**
     * 原始类名称
     */
    val rawClazzName: String = "",

    /**
     * 全限定的名称
     */
    val fullyQualifiedName: String = "",

    /**
     * 混淆过后的全限定的名称
     */
    var fullyObfuscateQualifiedName: String = "",
    /**
     * 混淆后的类名称
     */
    var obfuscateClazzName: String = "",

    var constructors: List<ConstructorInfo>,

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
    /**
     * 是否 interface
     */
    var isInterface: Boolean = false,
    /**
     * 是否枚举类
     */
    var isEnum: Boolean = false,
    /**
     * 所属的文件
     */
    var belongFile: File? = null,
    /**
     * 混淆过后的file名称
     */
    var belongFileObfuscateName: String = "",
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
    /**
     * 是对应的 javaClass
     */
    fun isCorrespondingJavaClass(javaClass: JavaClass): Boolean {
        return fullyQualifiedName == javaClass.fullyQualifiedName
    }

    fun isCorrespondingJavaType(javaType: JavaType): Boolean {
        return fullyQualifiedName == javaType.fullyQualifiedName
    }

    fun isBelongThisFile(file: File): Boolean {
        val belongFile = belongFile ?: return false
        return belongFile.absolutePath == file.absolutePath
    }

    fun obfuscateGenericCanonicalName(): String {
        return fullyObfuscateQualifiedName.replace('$', '.');
    }

    fun getClazzCanonicalName(): String {
        return fullyQualifiedName.replace("${packageName}.", "")
    }

    fun getObfuscateClazzCanonicalName(): String {
        return fullyObfuscateQualifiedName.replace("${packageName}.", "")
    }
}