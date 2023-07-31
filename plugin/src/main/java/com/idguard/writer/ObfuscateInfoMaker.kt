package com.idguard.writer

import com.idguard.modal.ClazzInfo
import com.idguard.modal.FieldInfo
import com.idguard.utils.findImportsClassInfo
import com.idguard.utils.findUpperNodes
import com.idguard.utils.replaceWords
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
        println("ob import $rawImports")
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
                    val raw = clazzinfo.fullyQualifiedName + "." + method.rawName
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
            } else {
                obfuscate.add(rawImport)
            }
        }
        obfuscate.sortedBy {
            it.length
        }
        println("af-ob import $obfuscate")
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
        val name = corrClassInfo.obfuscateClazzName
        val fields = corrClassInfo.fieldList
        copy.forEach {
            val javaConstructor = it as? DefaultJavaConstructor ?: return@forEach
            javaConstructor.name = name
            fields.forEach { field ->
                javaConstructor.sourceCode = javaConstructor.sourceCode.replaceWords(
                    "this.${field.name}",
                    "this.${field.obfuscateName}"
                ).replaceWords(field.name, field.obfuscateName)
            }
            result.add(javaConstructor)
        }
        return result
    }

    fun method(
        methods: List<JavaMethod>,
        currentClazzInfo: ClazzInfo,
        clazzInfos: List<ClazzInfo>
    ): List<JavaMethod> {

        //find all need replace field
        val needReplaceField = mutableListOf<FieldInfo>()
        val upperNodes = mutableListOf<ClazzInfo>()
        findUpperNodes(currentClazzInfo, upperNodes)
        val upperUsefulFields = upperNodes.flatMap { it.fieldList }.filter {
            it.modifier.isEmpty() || it.modifier.contains("public") || it.modifier.contains("protected")
        }
        needReplaceField.addAll(upperUsefulFields)
        needReplaceField.addAll(currentClazzInfo.fieldList)

        val r = findImportsClassInfo(currentClazzInfo, clazzInfos)
        println("r ${currentClazzInfo.rawClazzName} -> ${r.map { it.rawClazzName }}")

        val copy = methods.toList()
        val result = mutableListOf<JavaMethod>()
        copy.forEach {
            val javaMethod = it as? DefaultJavaMethod ?: return@forEach
            val methodInfo =
                currentClazzInfo.methodList.find { method ->
                    method.isCorrespondingJavaMethod(
                        javaMethod
                    )
                }
            methodInfo?.rawName?.let { name ->
                javaMethod.name = name
            }
            needReplaceField.forEach { field ->
                val raw = field.name
                val obfuscate = field.obfuscateName
                javaMethod.sourceCode = javaMethod.sourceCode.replaceWords(raw, obfuscate)
            }
            result.add(javaMethod)
        }
        return result
    }
}