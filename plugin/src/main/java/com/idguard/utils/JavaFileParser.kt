package com.idguard.utils

import com.idguard.modal.ClazzInfo
import com.idguard.modal.FieldInfo
import com.idguard.modal.MethodInfo
import com.thoughtworks.qdox.model.JavaClass

fun JavaClass.parser(): ClazzInfo {
    val clazzname = this.name
    val isInnerClass = this.isInner
    val isInterface = this.isInterface
    val isEnum = this.isEnum

    val obfuscateClazzName = RandomNameHelper.genClassName(Pair(4, 8))

    val methods = this.methods.map { javaMethod ->
        val isOverride = javaMethod.annotations.find {
            it.type.name.contains("Override")
        } != null
        val obfuscateName = if (isOverride) {
            ""
        } else {
            RandomNameHelper.genNames(1, Pair(4, 12), false, true).first()
        }
        MethodInfo(
            modifier = javaMethod.modifiers,
            name = javaMethod.name,
            returnType = javaMethod.returnType.genericValue,
            params = javaMethod.parameters.map { "${it.type} ${it.name}" },
            isOverride = isOverride,
            obfuscateName = obfuscateName
        )
    }

    val fields = fields.map { javaField ->
        val obfuscateName = RandomNameHelper.genNames(1, Pair(2, 8), false, true).first()
        FieldInfo(
            modifier = javaField.modifiers,
            name = javaField.name,
            type = javaField.type.name,
            obfuscateName = obfuscateName
        )
    }

    val packageName = packageName

    val implNames = implements.map {
        it.fullyQualifiedName
    }

    val extendName = if (superClass == null) {
        ""
    } else {
        superClass.fullyQualifiedName
    }

    val modifier = modifiers

    return ClazzInfo(
        modifier = modifier,
        packageName = packageName,
        rawClazzName = clazzname,
        obfuscateClazzName = obfuscateClazzName,
        methodList = methods,
        fieldList = fields,
        isInnerClass = isInnerClass,
        isInterface = isInterface,
        isEnum = isEnum,
        implName = implNames,
        extendName = extendName,
        bodyInfo = codeBlock,
    )
}