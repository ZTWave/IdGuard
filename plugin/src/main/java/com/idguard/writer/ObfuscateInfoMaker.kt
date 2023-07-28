package com.idguard.writer

import com.idguard.modal.ClazzInfo
import com.idguard.modal.FieldInfo
import com.idguard.modal.MethodInfo
import com.thoughtworks.qdox.model.JavaClass
import com.thoughtworks.qdox.model.JavaConstructor
import com.thoughtworks.qdox.model.JavaField
import com.thoughtworks.qdox.model.JavaMethod
import com.thoughtworks.qdox.model.JavaType
import com.thoughtworks.qdox.model.impl.DefaultJavaConstructor
import com.thoughtworks.qdox.model.impl.DefaultJavaField
import com.thoughtworks.qdox.model.impl.DefaultJavaMethod

object ObfuscateInfoMaker {
    fun imports(rawImports: List<String>, obInfos: List<ClazzInfo>): List<String> {
        //build all class maybe import quote names
        val mayImportsModality = mutableMapOf<String, String>()
        obInfos.forEach { clazzinfo ->
            //list all may be import statement
            val clazzLayerNames = clazzinfo.fullyQualifiedName
                .replace("${clazzinfo.packageName}.", "")
                .split(".")
            val clazzObfuscateLayerNames = clazzinfo.fullyObfuscateQualifiedName
                .replace("${clazzinfo.packageName}.", "")
                .split(".")

            val layerCount = clazzLayerNames.size

            for (layer in layerCount downTo 1) {
                val raw =
                    clazzinfo.packageName + "." + clazzLayerNames.take(layer).joinToString(".")
                val obfuscate =
                    clazzinfo.packageName + "." + clazzObfuscateLayerNames.take(layer)
                        .joinToString(".")

                mayImportsModality[raw] = obfuscate
            }
            //if clazz info has static method
            val staticMethodInfos = clazzinfo.methodList.filter { it.modifier.contains("static") }
            if (staticMethodInfos.isNotEmpty()) {
                for (method in staticMethodInfos) {
                    val raw = clazzinfo.fullyQualifiedName + "." + method.name
                    val obfuscate =
                        clazzinfo.fullyObfuscateQualifiedName + "." + method.obfuscateName
                    mayImportsModality[raw] = obfuscate
                }
            }

            //if clazz info has static field
            val staticFieldInfo = clazzinfo.fieldList.filter { it.modifier.contains("static") }
            if (staticFieldInfo.isNotEmpty()) {
                for (field in staticFieldInfo) {
                    val raw = "static " + clazzinfo.fullyQualifiedName + "." + field.name
                    val obfuscate =
                        "static " + clazzinfo.fullyObfuscateQualifiedName + "." + field.obfuscateName
                    mayImportsModality[raw] = obfuscate
                }
            }
        }

        val obfuscate = mutableListOf<String>()
        for (rawImport in rawImports) {
            val corrImport = mayImportsModality.getOrDefault(rawImport, "")
            if (corrImport.isNotBlank()) {
                obfuscate.add(corrImport)
            }
        }
        return obfuscate
    }

    fun className(cls: JavaClass, clazzInfos: List<ClazzInfo>): String {
        return clazzInfos.find { it.isCorrespondingJavaClass(cls) }?.obfuscateClazzName
            ?: throw RuntimeException("class ${cls.fullyQualifiedName} can not find in this given classInfos")
    }

    fun className(javaType: JavaType, clazzInfos: List<ClazzInfo>): String {
        return clazzInfos.find { it.isCorrespondingJavaType(javaType) }?.obfuscateClazzName
            ?: throw RuntimeException("class ${javaType.fullyQualifiedName} can not find in this given classInfos")
    }

    fun field(rawFields: List<JavaField>, obfuscateFieldInfo: List<FieldInfo>): List<JavaField> {
        val obfuscateFields = mutableListOf<JavaField>()
        val copy = rawFields.toList()
        copy.forEach { javaField ->
            val defaultJavaField = javaField as? DefaultJavaField ?: return@forEach
            val obFiledInfo =
                obfuscateFieldInfo.find { it.isCorrespondingJavaField(defaultJavaField) }
            obFiledInfo?.obfuscateName?.let {
                defaultJavaField.name = it
            }
            obfuscateFields.add(defaultJavaField)
        }
        return obfuscateFields
    }

    fun constructors(
        constructors: List<JavaConstructor>,
        corrClassInfo: ClazzInfo
    ): List<JavaConstructor> {
        val copy = constructors.toList()
        val result = mutableListOf<JavaConstructor>()
        val name = corrClassInfo.rawClazzName
        copy.forEach {
            val javaConstructor = it as? DefaultJavaConstructor ?: return@forEach
            javaConstructor.name = name
            result.add(javaConstructor)
        }
        return result
    }

    fun method(
        methods: List<JavaMethod>,
        obfuscateMethodsInfo: List<MethodInfo>
    ): List<JavaMethod> {
        val copy = methods.toList()
        val result = mutableListOf<JavaMethod>()
        copy.forEach {
            val javaMethod = it as? DefaultJavaMethod ?: return@forEach
            val methodInfo =
                obfuscateMethodsInfo.find { method -> method.isCorrespondingJavaMethod(javaMethod) }
            methodInfo?.name?.let { name ->
                javaMethod.name = name
            }
            result.add(javaMethod)
        }
        return result
    }
}