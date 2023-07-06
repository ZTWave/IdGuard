package com.idguard.utils

import java.io.File

/**
 * 对于  xxx.9.png
 * @return .9.png
 */
fun File.getExtensionName(): String {
    val a = name.replace(this.getRealName(), "")
    return a
}

/**
 * 对于 xxx.9.png xxx.png
 * @return xxx
 */
fun File.getRealName(): String = name.split(".").first()
