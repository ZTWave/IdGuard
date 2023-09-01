package com.idguard.utils

import com.idguard.modal.ClazzInfo
import com.idguard.modal.ConstructorInfo
import com.idguard.modal.FieldInfo
import com.idguard.modal.MethodInfo
import com.idguard.modal.OverrideStatusEnum
import com.idguard.modal.ParamInfo
import com.thoughtworks.qdox.model.JavaClass
import com.thoughtworks.qdox.model.JavaConstructor
import com.thoughtworks.qdox.model.JavaParameter

//only parser
fun JavaClass.parser(): ClazzInfo {
    val clazzname = this.name
    val isInterface = this.isInterface
    val isEnum = this.isEnum

    val parentJavaSource = parentSource

    val methods = this.methods.map { javaMethod ->
        val isOverride = javaMethod.annotations.find {
            it.type.name.contains("Override")
        } != null

        val isJsInterface = javaMethod.annotations.find {
            it.type.name.contains("JavascriptInterface")
        } != null

        val isNative = javaMethod.modifiers.contains("native")

        val needObfuscate = if (isOverride) {
            //temporary can't identify this method from our project or some library, jars
            //it will be fill after all java file is transform to clazzInfo
            OverrideStatusEnum.UN_CONFIRM
        } else if (isNative || isJsInterface) {
            //don't obfuscate native method
            OverrideStatusEnum.NOT_NEED
        } else {
            OverrideStatusEnum.NEED
        }

        MethodInfo(
            modifier = javaMethod.modifiers,
            rawName = javaMethod.name,
            returnType = javaMethod.returnType.genericValue,
            params = javaMethod.parameters.map { it.parser() },
            isOverride = isOverride,
            needObfuscate = needObfuscate,
            methodBody = javaMethod.codeBlock
        )
    }

    val fields = fields.map { javaField ->
        FieldInfo(
            modifier = javaField.modifiers,
            rawName = javaField.name,
            type = javaField.type.fullyQualifiedName,
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
        methodList = methods,
        fieldList = fields,
        isInterface = isInterface,
        isEnum = isEnum,
        implFullQualifiedName = implNames,
        extendFullQualifiedName = extendName,
        imports = parentJavaSource.imports,
        bodyInfo = codeBlock,
        constructors = constructors.map { it.parser() }
    )
}

fun JavaParameter.parser() = ParamInfo(
    type.fullyQualifiedName,
    name,
)

fun JavaConstructor.parser() = ConstructorInfo(params = parameters.map {
    it.parser()
})