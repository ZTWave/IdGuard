package com.idguard.modal

import com.idguard.utils.RandomNameHelper
import com.idguard.utils.getExtensionName
import com.idguard.utils.getPackagePath
import com.idguard.utils.getRealName
import java.io.File

class ClazzInfo {

    constructor(file: File) {
        fillObInfo(file)
    }

    /**
     * 包名称
     */
    var packageName: String = ""
        private set

    /**
     * 原始路径
     */
    var rawPath: String = ""
        private set

    /**
     * 混淆后的路径
     */
    var obfuscatePath: String = ""
        private set

    /**
     * 原始类名称
     */
    var rawClazzName: String = ""
        private set

    /**
     * 混淆后的类名称
     */
    var obfuscateClazzName: String = ""
        private set

    private fun fillObInfo(file: File) {
        rawPath = file.absolutePath
        rawClazzName = file.getRealName()
        val extensionName = file.getExtensionName()
        obfuscateClazzName = RandomNameHelper.genClassName(Pair(4, 8))
        obfuscatePath =
            file.parentFile.absolutePath + File.separator + obfuscateClazzName + extensionName
        packageName = rawPath.getPackagePath()
    }

    override fun toString(): String {
        val sb = StringBuilder("")
        sb.append("rawPath -> $rawPath")
        sb.append("\n")
        sb.append("obfuscatePath -> $obfuscatePath")
        sb.append("\n")
        sb.append("rawClazzName -> $rawClazzName")
        sb.append("\n")
        sb.append("obfuscateClazzName -> $obfuscateClazzName")
        sb.append("\n")
        sb.append("packageName -> $packageName")
        return sb.toString()
    }
}