package com.idguard.utils

/**
 * return this list has at least one element at the given list
 */
fun <E> List<E>.hasOneOf(list: List<E>, equalBlock: (E, E) -> Boolean): Boolean {
    this.forEach { e1 ->
        val result = list.find { e2 ->
            equalBlock.invoke(e1, e2)
        }
        if (result != null) {
            return true
        }
    }
    return false
}

/**
 * judge two list is equal
 */
fun <String> List<String>.elementEquals(list: List<String>): Boolean {
    if (this.size != list.size) {
        return false
    }
    this.forEach { string ->
        if (list.find { it == string } == null) {
            return false
        }
    }
    return true
}