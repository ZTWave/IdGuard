package com.idguard.utils

import com.idguard.modal.ClazzInfo
import com.idguard.modal.MethodInfo

fun findUpperNodes(rootNode: ClazzInfo, nodes: MutableList<ClazzInfo>) {
    val firstLayerNodes = mutableListOf<ClazzInfo>()
    firstLayerNodes.addAll(rootNode.implNodes.toMutableList())
    rootNode.extendNode?.let { firstLayerNodes.add(it) }
    if (firstLayerNodes.isEmpty()) {
        return
    }
    nodes.addAll(firstLayerNodes)
    firstLayerNodes.forEach {
        findUpperNodes(it, nodes)
    }
}

fun findImportsClassInfo(searchInfo: ClazzInfo, allInfos: List<ClazzInfo>): List<ClazzInfo> {
    val mayImportClazzInfo = mutableSetOf<ClazzInfo>()
    //current file imports
    val fileImports = searchInfo.imports

    val mayImportsModality = mutableListOf<String>()
    allInfos.forEach { classInfo ->
        val clazzLayerNames =
            classInfo.fullyQualifiedName.replace("${classInfo.packageName}.", "")
                .split(".")
        for (layer in clazzLayerNames.size downTo 1) {
            val assumeModality =
                classInfo.packageName + "." + clazzLayerNames.take(layer)
                    .joinToString(".")
            if (isNotSamePackage(classInfo, searchInfo)) {
                mayImportsModality.add(assumeModality)
            }
        }

        //import
        for (mayImport in mayImportsModality) {
            val importSplit = mayImport.split(".").toMutableList().apply {
                removeLast()
            }
            val importStartStr = importSplit.joinToString(".") + ".*"
            if (fileImports.contains(mayImport) || fileImports.contains(importStartStr)) {
                mayImportClazzInfo.add(classInfo)
            }
        }

        //same package
        if (searchInfo.packageName == classInfo.packageName && searchInfo.rawClazzName != classInfo.rawClazzName) {
            mayImportClazzInfo.add(classInfo)
        }

        // new com.littlew.example.pf.ia.C
        if (searchInfo.bodyInfo.contains(classInfo.fullyQualifiedName)) {
            mayImportClazzInfo.add(classInfo)
        }
    }
    return mayImportClazzInfo.toList()
}

fun findCanReplaceWordPair(
    rootClazzInfo: ClazzInfo,
    importClazzInfo: List<ClazzInfo>
): List<Pair<String, String>> {
    val replacePair = mutableListOf<Pair<String, String>>()
    importClazzInfo.forEach { clazzinfo ->
        //if clazz info has static method
        val staticMethodInfos = clazzinfo.methodList.filter { it.modifier.contains("static") }
        if (staticMethodInfos.isNotEmpty()) {
            for (method in staticMethodInfos) {
                val raw = clazzinfo.fullyQualifiedName + "." + method.rawName
                val obfuscate =
                    clazzinfo.fullyObfuscateQualifiedName + "." + method.obfuscateName

                val layerCount = raw.split(".").size

                for (count in layerCount downTo 1) {
                    val r = raw.split(".").takeLast(count).joinToString(".")
                    val o = obfuscate.split(".").takeLast(count).joinToString(".")
                    replacePair.add(Pair(r, o))
                }

            }
        }

        //if clazz info has static field
        val staticFieldInfo = clazzinfo.fieldList.filter { it.modifier.contains("static") }
        if (staticFieldInfo.isNotEmpty()) {
            for (field in staticFieldInfo) {
                val raw = clazzinfo.fullyQualifiedName + "." + field.rawName
                val obfuscate =
                    clazzinfo.fullyObfuscateQualifiedName + "." + field.obfuscateName
                val layerCount = raw.split(".").size
                for (count in layerCount downTo 1) {
                    val r = raw.split(".").takeLast(count).joinToString(".")
                    val o = obfuscate.split(".").takeLast(count).joinToString(".")
                    replacePair.add(Pair(r, o))
                }
            }
        }

        //class name
        //eg. com.littlew.example.pf.ia.C, littlew.example.pf.ia.C, example.pf.ia.C, pf.ia.C, ia.C, C
        if (clazzinfo.parentNode != null) {
            val layerArray = clazzinfo.fullyQualifiedName.split(".")
            val obLayerArray = clazzinfo.fullyObfuscateQualifiedName.split(".")
            val layerCount = layerArray.size
            for (count in layerCount downTo 1) {
                val r = layerArray.takeLast(count).joinToString(".")
                val o = obLayerArray.takeLast(count).joinToString(".")
                replacePair.add(Pair(r, o))
            }
        } else {
            replacePair.add(Pair(clazzinfo.rawClazzName, clazzinfo.obfuscateClazzName))
            replacePair.add(
                Pair(
                    clazzinfo.fullyQualifiedName,
                    clazzinfo.fullyObfuscateQualifiedName
                )
            )
        }

        //class public protect default field
        val methods = clazzinfo.methodList.filterNot {
            it.isStatic() || it.modifier.contains("private") ||
                if (isNotSamePackage(rootClazzInfo, clazzinfo)) {
                    //not same package
                    it.modifier.isEmpty() || it.modifier.contains("protect")
                } else {
                    true
                }
        }
        methods.forEach { methodInfo ->
            replacePair.add(Pair(methodInfo.rawName, methodInfo.obfuscateName))
        }

        //class public protect default method
        val fields = clazzinfo.fieldList.filterNot {
            it.isStatic() || it.modifier.contains("private") ||
                if (isNotSamePackage(rootClazzInfo, clazzinfo)) {
                    //not same package
                    it.modifier.isEmpty() || it.modifier.contains("protect")
                } else {
                    true
                }
        }
        fields.forEach { fieldInfo ->
            replacePair.add(Pair(fieldInfo.rawName, fieldInfo.obfuscateName))
        }
    }
    return replacePair.sortedByDescending { it.first.length }
}

fun findCanReplaceDotPair(
    currentClazzInfo: ClazzInfo,
    mayImportClassInfo: List<ClazzInfo>
): List<Pair<String, String>> {
    val methodPair = mutableListOf<Pair<String, String>>()
    val fieldPair = mutableListOf<Pair<String, String>>()
    mayImportClassInfo.forEach { clazzInfo ->
        val methods = clazzInfo.methodList
        methods.forEach methodForEach@{ method: MethodInfo ->
            if (method.isStatic() || method.obfuscateName.isBlank()) {
                return@methodForEach
            }
            if (isSamePackage(clazzInfo, currentClazzInfo)) {
                if (method.isSamePackageVisible()) {
                    methodPair.add(Pair(method.rawName, method.obfuscateName))
                }
            } else {
                //not same package
                if (method.isNotSamePackageVisible()) {
                    methodPair.add(Pair(method.rawName, method.obfuscateName))
                }
            }
        }

        val fields = clazzInfo.fieldList
        fields.forEach methodForEach@{ field ->
            if (field.isStatic()) {
                return@methodForEach
            }
            if (isSamePackage(clazzInfo, currentClazzInfo)) {
                if (field.isSamePackageVisible()) {
                    fieldPair.add(Pair(field.rawName, field.obfuscateName))
                }
            } else {
                //not same package
                if (field.isNotSamePackageVisible()) {
                    fieldPair.add(Pair(field.rawName, field.obfuscateName))
                }
            }
        }
    }

    val resultPair = mutableListOf<Pair<String, String>>()
    resultPair.addAll(
        //a.b()
        methodPair.map { Pair(".${it.first}(", ".${it.second}(") }
    )
    resultPair.addAll(
        //a.b.
        //maybe replace package name .b. in some times
        fieldPair.map { Pair(".${it.first}.", ".${it.second}.") }
    )
    resultPair.addAll(
        //a.b;
        fieldPair.map { Pair(".${it.first};", ".${it.second};") }
    )
    resultPair.addAll(
        //(a.b)
        fieldPair.map { Pair(".${it.first})", ".${it.second})") }
    )
    resultPair.addAll(
        //(a.b,a.c)
        fieldPair.map { Pair(".${it.first},", ".${it.second},") }
    )

    println("class ${currentClazzInfo.fullyQualifiedName} need replace")
    println(resultPair)

    return resultPair
}

private fun isSamePackage(
    clazz1: ClazzInfo,
    clazz2: ClazzInfo
) = !isNotSamePackage(clazz1, clazz2)

private fun isNotSamePackage(
    clazz1: ClazzInfo,
    clazz2: ClazzInfo
) = clazz1.packageName != clazz2.packageName
