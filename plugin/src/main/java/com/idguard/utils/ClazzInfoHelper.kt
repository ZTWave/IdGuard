package com.idguard.utils

import com.idguard.modal.ClazzInfo

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
            if (classInfo.packageName != searchInfo.packageName) {
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

fun findCanReplacePair(
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
        }

        //class public protect default field
        val methods = clazzinfo.methodList.filterNot {
            it.modifier.contains("static") || it.modifier.contains("private") ||
                if (rootClazzInfo.packageName != clazzinfo.packageName) {
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
            it.modifier.contains("static") || it.modifier.contains("private") ||
                if (rootClazzInfo.packageName != clazzinfo.packageName) {
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