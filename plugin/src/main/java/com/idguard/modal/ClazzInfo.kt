package com.idguard.modal

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
    val obfuscatePath: String = "",
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
    val parentNode: ClazzInfo? = null,

    var isInnerClass: Boolean = false,

    var isNestedClass :Boolean = false,

    var isInterface: Boolean = false
)