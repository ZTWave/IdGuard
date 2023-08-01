package com.idguard.utils

import com.idguard.modal.ClazzInfo
import com.idguard.modal.FieldInfo
import com.idguard.modal.MethodInfo
import com.thoughtworks.qdox.model.JavaClass

fun JavaClass.parser(): ClazzInfo {
    val clazzname = this.name
    val isInterface = this.isInterface
    val isEnum = this.isEnum

    val parentJavaSource = parentSource

    val obfuscateClazzName = getOrGenClassObfuscateName(clazzname)

    val methods = this.methods.map { javaMethod ->
        val isOverride = javaMethod.annotations.find {
            it.type.name.contains("Override")
        } != null
        val obfuscateName = if (isOverride) {
            //temporary can't identify this method from our project or some library, jars
            //it will be fill after all java file is transform to clazzInfo
            ""
        } else if (javaMethod.modifiers.contains("native")) {
            //don't obfuscate native method
            javaMethod.name
        } else {
            getOrGenMethodObfuscateName(javaMethod.name)
        }
        MethodInfo(
            modifier = javaMethod.modifiers,
            rawName = javaMethod.name,
            returnType = javaMethod.returnType.genericValue,
            params = javaMethod.parameters.map { "${it.type} ${it.name}" },
            isOverride = isOverride,
            obfuscateName = obfuscateName,
            methodBody = javaMethod.codeBlock
        )
    }

    val fields = fields.map { javaField ->
        val obfuscateName = getOrGenFieldObfuscateName(javaField.name)
        FieldInfo(
            modifier = javaField.modifiers,
            rawName = javaField.name,
            type = javaField.type.fullyQualifiedName,
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
        fullyQualifiedName = fullyQualifiedName,
        obfuscateClazzName = obfuscateClazzName,
        methodList = methods,
        fieldList = fields,
        isInterface = isInterface,
        isEnum = isEnum,
        implFullQualifiedName = implNames,
        extendFullQualifiedName = extendName,
        imports = parentJavaSource.imports,
        bodyInfo = codeBlock,
    )
}

/**
 * raw an obfuscate name cache
 */
private val clazzNameObMap = mutableMapOf<String, String>()
private val fieldNameObMap = mutableMapOf<String, String>()
private val methodNameObMap = mutableMapOf<String, String>()

private fun getOrGenClassObfuscateName(rawName: String): String {
    return clazzNameObMap.getOrElse(rawName) {
        RandomNameHelper.genClassName(Pair(4, 8))
    }
}

private fun getOrGenFieldObfuscateName(rawName: String): String {
    return fieldNameObMap.getOrElse(rawName) {
        RandomNameHelper.genNames(1, Pair(2, 8), false, true).first()
    }
}

private fun getOrGenMethodObfuscateName(rawName: String): String {
    return methodNameObMap.getOrElse(rawName) {
        RandomNameHelper.genNames(1, Pair(4, 12), false, true).first()
    }
}