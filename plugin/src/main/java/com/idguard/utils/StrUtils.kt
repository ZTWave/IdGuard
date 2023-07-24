package com.idguard.utils

import java.io.File

fun String.isWord(index: Int, oldValue: String): Boolean {
    val firstChar = oldValue[0].code
    if (index > 0 && (firstChar in 65..90 || firstChar == 95 || firstChar in 97..122)) {
        val prefix = get(index - 1).code
        // $ . 0-9 A-Z _ a-z
        if (prefix == 36 || prefix == 46 || prefix in 48..57 || prefix in 65..90 || prefix == 95 || prefix in 97..122) {
            return false
        }
    }
    val endChar = oldValue[oldValue.lastIndex].code
    // $ 0-9 A-Z _ a-z
    if (endChar == 36 || endChar in 48..57 || endChar in 65..90 || endChar == 95 || endChar in 97..122) {

        val suffix = getOrNull(index + oldValue.length)?.code
        // $ 0-9 A-Z _ a-z
        if (suffix == 36 || suffix in 48..57 || suffix in 65..90 || suffix == 95 || suffix in 97..122) {
            return false
        }
    }
    return true
}

fun String.replaceWords(
    oldValue: String,
    newValue: String,
    ignoreCase: Boolean = false
): String {
    var occurrenceIndex: Int = indexOf(oldValue, 0, ignoreCase)
//        println("replace $oldValue -> $newValue ocindex => $occurrenceIndex")
    // FAST PATH: no match
    if (occurrenceIndex < 0) return this

    val oldValueLength = oldValue.length
    val searchStep = oldValueLength.coerceAtLeast(1)
    val newLengthHint = length - oldValueLength + newValue.length
    if (newLengthHint < 0) throw OutOfMemoryError()
    val stringBuilder = StringBuilder(newLengthHint)

    var i = 0
    do {
        if (isWord(occurrenceIndex, oldValue)) {
            stringBuilder.append(this, i, occurrenceIndex).append(newValue)
        } else {
            stringBuilder.append(this, i, occurrenceIndex + oldValueLength)
        }
        i = occurrenceIndex + oldValueLength
        if (occurrenceIndex >= length) break
        occurrenceIndex = indexOf(oldValue, occurrenceIndex + searchStep, ignoreCase)
    } while (occurrenceIndex > 0)
    return stringBuilder.append(this, i, length).toString()
}

/**
 * 通过文件的绝对路径获取文件名称
 */
fun String.getFileName(): String {
    val name = this.split(File.separator).lastOrNull()
        ?: throw RuntimeException("file path is error $this")
    return name.split('.')[0]
}

fun String.splitWords(): List<String> {
    val regex = Regex("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])")
    return split(regex).map { it.lowercase() }
}

/**
 * 获取对应文件的package路径
 * 已废弃
 */
@Deprecated("use file read line to find lines start with package")
fun String.getPackagePath(): String {
    val file = File(this)
    if (file.isDirectory) {
        throw IllegalArgumentException("file path is a directory")
    }
    val filePath = file.absolutePath
    val splitPathArray = filePath.split(File.separator)
    val indexOfJava = splitPathArray.indexOfFirst { it == "java" }
    if (indexOfJava < 0) {
        throw IllegalArgumentException("file path is not correct path is $filePath")
    }
    val packagePathArray = splitPathArray.subList(indexOfJava + 1, splitPathArray.size - 1)
    return packagePathArray.joinToString(".")
}