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

fun String.isStartWithDotWord(index: Int, oldValue: String): Boolean {
    val firstChar = oldValue[0].code
    if (index > 0 && (firstChar in 65..90 || firstChar == 95 || firstChar in 97..122)) {
        val prefix = get(index - 1).code
        // .
        if (prefix != 46) {
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


fun String.replaceStartWithDot(oldValue: String, newValue: String): String {
    var occurrenceIndex: Int = indexOf(oldValue, 0, false)
    if (occurrenceIndex < 0) return this

    val oldValueLength = oldValue.length
    val searchStep = oldValueLength.coerceAtLeast(1)
    val newLengthHint = length - oldValueLength + newValue.length
    if (newLengthHint < 0) throw OutOfMemoryError()
    val stringBuilder = StringBuilder(newLengthHint)
    var i = 0
    do {
        if (isStartWithDotWord(occurrenceIndex, oldValue)
            && !isRReference(occurrenceIndex, oldValue)
        ) {
            stringBuilder.append(this, i, occurrenceIndex).append(newValue)
        } else {
            stringBuilder.append(this, i, occurrenceIndex + oldValueLength)
        }
        i = occurrenceIndex + oldValueLength
        if (occurrenceIndex >= length) break
        occurrenceIndex = indexOf(oldValue, occurrenceIndex + searchStep, false)
    } while (occurrenceIndex > 0)
    return stringBuilder.append(this, i, length).toString()
}

/**
 * R 引用
 */
private val Rreference = listOf<String>(
    "R.id.",
    "R.string.",
    "R.drawable.",
    "R.mipmap.",
    "R.raw.",
    "R.anim.",
    "R.array.",
    "R.menu.",
    "R.style.",
    "R.layout.",
    "R.color.",
    "R.attr.",
    "R.bool.",
    "R.dimen.",
    "R.xml.",
    "R.styleable.",
    "R.animator.",
    "R.integer.",
    "R.interpolator.",
)

fun String.isRReference(index: Int, oldValue: String): Boolean {
    if (index <= 0) {
        return false
    }
    Rreference.forEach { ref ->
        if (index - ref.length < 0) {
            return false
        }

        //R.id.abc
        //R.id
        //012345
        val detectStr = substring(index - ref.length, index)
        if (detectStr == ref) {
            return true
        }
    }
    return false
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