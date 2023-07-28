package com.idguard.modal

import com.idguard.utils.elementEquals
import com.thoughtworks.qdox.model.JavaField

data class FieldInfo(
    val name: String = "",
    val type: String = "",
    val modifier: List<String> = emptyList(),
    var obfuscateName: String = ""
) {
    fun isCorrespondingJavaField(javaField: JavaField): Boolean {
        return name == javaField.name
            && type == javaField.type.fullyQualifiedName
            && modifier.elementEquals(javaField.modifiers)
    }
}