package com.idguard.modal

import com.idguard.utils.elementEquals
import com.idguard.utils.hasOneOf
import com.thoughtworks.qdox.model.JavaField

data class FieldInfo(
    val rawName: String = "",
    val type: String = "",
    val modifier: List<String> = emptyList(),
    var obfuscateName: String = ""
) {
    fun isCorrespondingJavaField(javaField: JavaField): Boolean {
        return rawName == javaField.name
            && type == javaField.type.fullyQualifiedName
            && modifier.elementEquals(javaField.modifiers)
    }

    fun isStatic(): Boolean {
        return modifier.contains("static")
    }

    fun isSamePackageVisible(): Boolean {
        val detectedModifier = listOf("public", "protected") //or no modifier
        return modifier.hasOneOf(detectedModifier) { s: String, s2: String ->
            s == s2
        } || modifier.isEmpty()
    }

    fun isNotSamePackageVisible():Boolean{
        return modifier.contains("public")
    }
}