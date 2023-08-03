package com.idguard.writer

import com.idguard.modal.ClazzInfo
import com.idguard.modal.FieldInfo
import com.idguard.utils.RandomNameHelper
import com.idguard.utils.findCanReplaceDotPair
import com.idguard.utils.findCanReplaceWordPair
import com.idguard.utils.findImportsClassInfo
import com.idguard.utils.findUpperNodes
import com.idguard.utils.replaceWords
import com.thoughtworks.qdox.model.JavaClass
import com.thoughtworks.qdox.model.JavaConstructor
import com.thoughtworks.qdox.model.JavaField
import com.thoughtworks.qdox.model.JavaMethod
import com.thoughtworks.qdox.model.JavaParameter
import com.thoughtworks.qdox.model.JavaType
import com.thoughtworks.qdox.model.impl.DefaultJavaConstructor
import com.thoughtworks.qdox.model.impl.DefaultJavaField
import com.thoughtworks.qdox.model.impl.DefaultJavaMethod
import com.thoughtworks.qdox.model.impl.DefaultJavaParameter

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
                    val raw = "static " + clazzinfo.fullyQualifiedName + "." + method.rawName
                    val obfuscate =
                        "static " + clazzinfo.fullyObfuscateQualifiedName + "." + method.obfuscateName
                    mayImportsModality[raw] = obfuscate
                }
            }

            //if clazz info has static field
            val staticFieldInfo = clazzinfo.fieldList.filter { it.modifier.contains("static") }
            if (staticFieldInfo.isNotEmpty()) {
                for (field in staticFieldInfo) {
                    val raw = "static " + clazzinfo.fullyQualifiedName + "." + field.rawName
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

    fun field(rawFields: List<JavaField>, obfuscateFieldInfo: List<FieldInfo>): List<JavaField> {
        val obfuscateFields = mutableListOf<JavaField>()
        val copy = rawFields.toSet()
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
                    "this.${field.rawName}",
                    "this.${field.obfuscateName}"
                ).replaceWords(field.rawName, field.obfuscateName)
            }
            result.add(javaConstructor)
        }
        return result
    }

    fun methods(
        methods: List<JavaMethod>,
        currentClazzInfo: ClazzInfo,
        clazzInfos: List<ClazzInfo>
    ): Set<JavaMethod> {

        //find all need replace field
        val needReplaceField = mutableListOf<FieldInfo>()
        val upperNodes = mutableListOf<ClazzInfo>()
        findUpperNodes(currentClazzInfo, upperNodes)
        val upperUsefulFields = upperNodes.flatMap { it.fieldList }.filter {
            it.modifier.isEmpty() || it.modifier.contains("public") || it.modifier.contains("protected")
        }
        //upper nodes fields
        needReplaceField.addAll(upperUsefulFields)

        //current node fields
        needReplaceField.addAll(currentClazzInfo.fieldList)

        val needReplaceCurrentMethod =
            currentClazzInfo.methodList.map { Pair(it.rawName, it.obfuscateName) }

        val mayImportClassInfo = findImportsClassInfo(currentClazzInfo, clazzInfos)
        val replaceMap = findCanReplaceWordPair(currentClazzInfo, mayImportClassInfo)

        //use replace method
        val replaceClassDotMap = findCanReplaceDotPair(currentClazzInfo, mayImportClassInfo)

        val copy = methods.toSet()
        val result = mutableSetOf<JavaMethod>()
        copy.forEach {
            val javaMethod = it as? DefaultJavaMethod ?: return@forEach

            //name
            val methodInfo =
                currentClazzInfo.methodList.find { method ->
                    method.isCorrespondingJavaMethod(
                        javaMethod
                    )
                }
            val newName = if (methodInfo?.obfuscateName.isNullOrBlank()) {
                methodInfo?.rawName
            } else {
                methodInfo?.obfuscateName
            }
            newName?.let {
                javaMethod.name = it
            }

            //source code replace
            if (methodInfo?.isOverride == true && methodInfo.obfuscateName.isNotBlank()) {
                //super. block
                javaMethod.sourceCode = javaMethod.sourceCode.replaceWords(
                    "super.${methodInfo.rawName}",
                    "super.${methodInfo.obfuscateName}",
                )
            }
            //fields
            needReplaceField.forEach { field ->
                val raw = field.rawName
                val obfuscate = field.obfuscateName
                javaMethod.sourceCode = javaMethod.sourceCode.replaceWords(raw, obfuscate)
            }

            //current class method
            needReplaceCurrentMethod.forEach { (r, o) ->
                javaMethod.sourceCode = javaMethod.sourceCode.replaceWords(r, o)
            }

            //declaration
            //eg. XXX x = new XXX; replace XXX
            replaceMap.forEach { (raw, obfuscate) ->
                javaMethod.sourceCode = javaMethod.sourceCode.replaceWords(raw, obfuscate)
            }

            replaceClassDotMap.forEach { (r, o) ->
                javaMethod.sourceCode = javaMethod.sourceCode.replace(r, o)
            }

            result.add(javaMethod)
        }
        return result
    }

    private fun sortDescendingClazzInfoQualifiedNamePairs(clazzInfos: List<ClazzInfo>): List<Pair<String, String>> {
        val pairs = mutableListOf<Pair<String, String>>()
        clazzInfos.forEach { clazzInfo ->
            pairs.add(Pair(clazzInfo.fullyQualifiedName, clazzInfo.fullyObfuscateQualifiedName))
        }
        pairs.sortByDescending {
            it.first.length
        }
        return pairs
    }

    fun returnTypeName(type: JavaType, clazzInfos: List<ClazzInfo>): String {
        var result = type.genericCanonicalName
        val pairs = sortDescendingClazzInfoQualifiedNamePairs(clazzInfos)
        pairs.forEach { (o, b) ->
            result = result.replace(o, b)
        }
        return result
    }

    fun exceptionGenericCanonicalName(
        exceptionJavaClass: JavaClass,
        clazzInfos: List<ClazzInfo>
    ): String {
        val correspondingClassInfo =
            clazzInfos.find { it.isCorrespondingJavaClass(exceptionJavaClass) }
                ?: return exceptionJavaClass.genericCanonicalName
        return correspondingClassInfo.obfuscateGenericCanonicalName()
    }

    fun superClassName(superClass: JavaType, clazzInfos: List<ClazzInfo>): String {
        var str = superClass.genericCanonicalName
        val pairs = sortDescendingClazzInfoQualifiedNamePairs(clazzInfos)
        pairs.forEach { (o, b) ->
            str = str.replace(o, b)
        }
        return str
    }

    fun implClassName(javaType: JavaType, clazzInfos: List<ClazzInfo>): String {
        var str = javaType.genericCanonicalName
        val pairs = sortDescendingClazzInfoQualifiedNamePairs(clazzInfos)
        pairs.forEach { (o, b) ->
            str = str.replace(o, b)
        }
        return str
    }

    fun parameterTypeName(parameter: JavaParameter, clazzInfos: List<ClazzInfo>): String {
        var str = parameter.genericCanonicalName
        val pairs = sortDescendingClazzInfoQualifiedNamePairs(clazzInfos)
        pairs.forEach { (o, b) ->
            str = str.replace(o, b)
        }
        return str
    }

    fun parametersName(parameters: List<JavaParameter>): Pair<List<JavaParameter>, Map<String, String>> {
        val copy = parameters.toList()
        val obNamesMap = mutableMapOf<String, String>()
        val obNameList = RandomNameHelper.genNames(
            parameters.size, Pair(2, 5), allLetter = true, isFirstLetter = true
        )
        parameters.forEachIndexed { index, javaParameter ->
            val defaultJavaParameter = javaParameter as? DefaultJavaParameter
            defaultJavaParameter?.let {
                val obName = obNameList[index]
                obNamesMap[it.name] = obName
                it.name = obName
            }
        }
        return Pair(copy, obNamesMap)
    }

    /**
     * for normal method and constructor method
     * only update method params
     * @param raw raw method content
     * @param replaceMap raw : obfuscate replace map
     */
    fun sourceCode(raw: String, replaceMap: Map<String, String>): String {
        var obfuscate = raw
        replaceMap.forEach { (r, o) ->
            obfuscate = obfuscate.replaceWords(r, o)
        }
        return obfuscate
    }

    /**
     * support generic only use string replace
     */
    fun fieldClazzName(field: JavaField, clazzInfos: List<ClazzInfo>): String {
        var obfuscateName = field.type.genericCanonicalName
        val pairs = sortDescendingClazzInfoQualifiedNamePairs(clazzInfos)
        pairs.forEach { (o, b) ->
            obfuscateName = obfuscateName.replace(o, b)
        }
        return obfuscateName
    }
}