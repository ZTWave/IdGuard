package com.idguard.utils

import java.io.File

/**
 * 对于  xxx.9.png
 * @return .9.png
 */
fun File.getExtensionName(): String {
    return name.replace(getRealName(), "")
}

/**
 * 对于 xxx.9.png xxx.png
 * @return xxx
 */
fun File.getRealName(): String = name.split(".").first()

/**
 * 获取file中第一行的 package
 */
fun File.packagePath(): String {
    val fileLines = readLines()
    return fileLines.find { it.startsWith("package ") }?.removePrefix("package ")?.removeSuffix(";")
        ?: ""
}