package com.idguard.modal

import java.io.File

/**
 * 目前只针对java文件
 * interface 和 class 类
 */
data class ClazzInfo(
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
     * 混淆后的类名称
     */
    val obfuscateClazzName: String = "",
    /**
     * 参数集合
     */
    val fieldList: List<FieldInfo> = emptyList(),
    /**
     * 方法集合
     */
    val methodList: List<FunInfo> = emptyList(),
    /**
     * 被嵌套的那个类
     */
    var parentNode: ClazzInfo? = null,

    /**
     * 所继承的类的名称
     */
    val extendName: String = "",
    /**
     * 继承的那个节点
     */
    var extendNode: ClazzInfo? = null,
    /**
     * 实现了哪个接口的name
     */
    val implName: List<String> = emptyList(),
    /**
     * 实现了哪个接口的节点
     */
    val implNode: ClazzInfo? = null,

    var isInnerClass: Boolean = false,

    var isNestedClass: Boolean = false,

    var isInterface: Boolean = false,

    /**
     * 所属的文件
     */
    val belongFile: File,

    /**
     * 引入的包 不包括正文代码中以 com.a.b.c 形式引入的
     */
    val imports: List<String> = emptyList(),

    /**
     * class 内容
     */
    val bodyInfo: String,
)