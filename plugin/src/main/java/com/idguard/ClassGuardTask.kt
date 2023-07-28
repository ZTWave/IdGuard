package com.idguard

import com.idguard.modal.ClazzInfo
import com.idguard.modal.MethodInfo
import com.idguard.utils.*
import com.thoughtworks.qdox.JavaProjectBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

open class ClassGuardTask @Inject constructor(
    private val variantName: String,
) : DefaultTask() {

    init {
        group = "guard"
    }

    private val javaSrcPath =
        project.projectDir.absolutePath + File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator
    private val javaFileExtensionName = ".java"
    private val clazzInfoList = mutableListOf<ClazzInfo>()
    private val funNameMapping = mutableMapOf<String, String>()

    private val mappingName = "class_guard_mapping.text"

    /**
     * only for java source file
     */
    @TaskAction
    fun execute() {
        val javaFile = project.javaDirs(variantName)
        val javaFilesTree = project.files(javaFile).asFileTree
        println("src path -> $javaSrcPath")
        val javaSrc = File(javaSrcPath)
        if (!javaSrc.exists()) {
            throw RuntimeException("java src -> $javaFile is not exits")
        }
        val javaProjectBuilder = JavaProjectBuilder().apply {
            addSourceTree(javaSrc)
        }
        println("class size -> ${javaProjectBuilder.classes.size} module size -> ${javaProjectBuilder.modules.size}")
        fillClazzInfoBelongFile(javaProjectBuilder, javaFilesTree)
        println("class info parent nested class analyze finished.")

        println("start find class extend and implements node...")
        relatedClazzNodes(javaProjectBuilder)
        println("class info extend and implement analyze finished.")

        println("start fill override fun obfuscate name...")
        //find no obfuscate override method name and fill it
        fillMethodObInfo()
        println("fill override fun obfuscate name finished.")

        println("start fill obfuscated fully qualified name...")
        fillObfuscateFullyQualifiedName()
        println("fill obfuscated fully qualified name finished.")

        /*println("start obfuscate...")
        obfuscateJavaFile(javaFilesTree)
        //manifest 内容替换
        updateManifest()

        //替换layout中的自定义view
        updateLayoutXml()*/

        printInfos()

        //outputMapping()
    }

    private fun printInfos() {
        clazzInfoList.forEach {
            println("${it.modifier} ${it.rawClazzName} extend ${it.extendNode?.fullyQualifiedName} implements ${it.implNodes.map { node -> node.fullyQualifiedName }}")
            println("parent -> ${it.parentNode?.rawClazzName ?: "null"} ")
            println("packageName -> ${it.packageName}")
            println("import -> ${it.imports}")
            println("fullyQualifiedName -> ${it.fullyQualifiedName} obfuscateQualifiedName -> ${it.fullyObfuscateQualifiedName}")
            println("file -> ${it.belongFile} belongFileObfuscateName -> ${it.belongFileObfuscateName}")
            val methodPrint =
                it.methodList.map { me -> "${me.name} -> ${me.obfuscateName} \n ${me.methodBody}" }
            println("method -> $methodPrint")
            val fieldsPrint =
                it.fieldList.map { field -> "${field.name} -> ${field.obfuscateName}" }
            println("fields -> $fieldsPrint")
            println("class body")
            println(it.bodyInfo)
            println()
        }
    }

    private fun fillObfuscateFullyQualifiedName() {
        clazzInfoList.forEach { clazzInfo ->
            val clazzNodes = mutableListOf<ClazzInfo>()

            //find all parent nodes and this already has been sorted form innermost to outermost
            var checkingNode = clazzInfo
            while (checkingNode.parentNode != null) {
                val parentNode = checkingNode.parentNode!!
                clazzNodes.add(parentNode)
                checkingNode = parentNode
            }
            //reverse it
            clazzNodes.reverse()
            //add self
            clazzNodes.add(clazzInfo)

            val clazzQuoteName = clazzNodes.map { it.obfuscateClazzName }.joinToString(".")
            println("clazzinfo -> ${clazzInfo.fullyQualifiedName} quotename -> $clazzQuoteName")
            clazzInfo.fullyObfuscateQualifiedName = clazzInfo.packageName + "." + clazzQuoteName
        }
    }

    private fun fillClazzInfoBelongFile(
        javaProjectBuilder: JavaProjectBuilder,
        javaSourceFileTree: FileTree
    ) {
        javaProjectBuilder.classes.forEach { javaClass ->
            val clazzInfo = javaClass.parser()
            val packageAbsolutePath =
                javaSrcPath + clazzInfo.packageName.replaceWords(".", File.separator)
            println("filePath -> $packageAbsolutePath")
            val leftPath = clazzInfo.fullyQualifiedName.replace(clazzInfo.packageName, "")
            //package name can't same as class name
            val fileName = leftPath.split(".").filterNot { t -> t.isBlank() }[0]
            println("fileName -> $fileName")
            val sourceFile =
                File(packageAbsolutePath + File.separator + fileName + javaFileExtensionName)
            if (sourceFile.exists()) {
                clazzInfo.belongFile = sourceFile
                clazzInfoList.add(clazzInfo)
            } else {
                //this class not in a java file name class body, find class statement in this path
                val searchPathFiles = project.files(packageAbsolutePath).asFileTree
                searchPathFiles.forEach { file ->
                    val content = file.readText()
                    val classModifiers = clazzInfo.modifier.joinToString { "" }
                    val classKey = if (clazzInfo.isEnum) {
                        "enum"
                    } else if (clazzInfo.isInterface) {
                        "interface"
                    } else {
                        "class"
                    }
                    val classFeatureStr =
                        "$classModifiers $classKey ${clazzInfo.rawClazzName}".trim()
                    if (content.contains(classFeatureStr)) {
                        println("find class feature str -> $classFeatureStr in file -> $file")
                        clazzInfo.belongFile = file
                        clazzInfoList.add(clazzInfo)
                    }
                }
            }
            if (clazzInfo.belongFile == null) {
                println("error find class in all files -> ${clazzInfo.rawClazzName}")
            }
        }
        //fill has belong file obfuscate name
        javaSourceFileTree.forEach { javaFile ->
            //this file contain class info
            val fileClazzInfos = clazzInfoList.filter { it.isBelongThisFile(javaFile) }
            //find same name class name as this file name
            val clazzInfo = fileClazzInfos.find { it.rawClazzName == javaFile.getRealName() }
            //get or gen class info obfuscate name
            val obfuscateFileName =
                clazzInfo?.obfuscateClazzName ?: RandomNameHelper.genClassName(Pair(4, 8))
            //fill belong file obfuscate name
            fileClazzInfos.forEach { clazzInfo ->
                clazzInfo.belongFileObfuscateName = obfuscateFileName
            }
        }
    }

    private fun findUpperNodes(rootNode: ClazzInfo, nodes: MutableList<ClazzInfo>) {
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

    private fun fillMethodObInfo() {
        clazzInfoList.forEach { clazzInfo ->
            val overrideMethods = clazzInfo.methodList.filter { it.isOverride }
            if (overrideMethods.isEmpty()) {
                return@forEach
            }
            //parent node methods expect override
            val parentMethods = mutableListOf<MethodInfo>()
            findUpperNodeMethods(clazzInfo, parentMethods)
            println()
            println("base -> ${clazzInfo.packageName}.${clazzInfo.rawClazzName} parent nodes method except override -> ${parentMethods.map { it.name }}")
            clazzInfo.methodList.forEach { method ->
                val find = parentMethods.find { it.isSameParams(method) }
                find?.let {
                    method.obfuscateName = it.obfuscateName
                }
            }
        }
    }

    private fun findUpperNodeMethods(
        rootNode: ClazzInfo,
        methodResult: MutableList<MethodInfo>
    ) {
        val nodes = mutableListOf<ClazzInfo>()
        nodes.addAll(rootNode.implNodes.toMutableList())
        rootNode.extendNode?.let { nodes.add(it) }
        if (nodes.isEmpty()) {
            return
        }
        val methods = nodes.flatMap { it.methodList }.filter { !it.isOverride }
        methodResult.addAll(methods)
        nodes.forEach {
            findUpperNodeMethods(it, methodResult)
        }
    }

    /**
     * 标识类的嵌套类或者接口的第一级继承和实现的关系
     */
    private fun relatedClazzNodes(javaProjectBuilder: JavaProjectBuilder) {
        //establishing extend and implements node relation
        javaProjectBuilder.classes.forEach { javaClass ->
            //this time checking node
            val node =
                clazzInfoList.find { it.isCorrespondingJavaClass(javaClass) } ?: return@forEach

            //establishing nested class node relation
            val nestedClass = javaClass.nestedClasses.toList()
            if (nestedClass.isNotEmpty()) {
                nestedClass.forEach { childJavaClass ->
                    val childNode =
                        clazzInfoList.find { it.isCorrespondingJavaClass(childJavaClass) }
                    childNode?.parentNode = node
                }
            }

            //find extend node
            val superClass = javaClass.superJavaClass
            //may be null
            superClass?.run {
                val extendNode = clazzInfoList.find { it.isCorrespondingJavaClass(this) }
                //assignment this node's extend node
                node.extendNode = extendNode
            }

            //find implement nodes
            val implements = javaClass.interfaces.toList()
            if (implements.isNotEmpty()) {
                val implementNode = implements.mapNotNull { implementJavaClass ->
                    clazzInfoList.find { it.isCorrespondingJavaClass(implementJavaClass) }
                }
                node.implNodes.addAll(implementNode)
            }
        }
    }

    /*private fun outputMapping() {
        val outputMap = clazzInfoList.map {
            val info = it.value
            val raw = info.packageName + "." + info.rawClazzName
            val obfuscate = info.packageName + "." + info.obfuscateClazzName
            raw to obfuscate
        }.toMap()
        MappingOutputHelper.write(project, mappingName, outputMap)
    }*/

    private fun obfuscateJavaFile(javaFileTree: FileTree) {
        //sorted by long qualified name can avoid many accidents
        val clazzInfoListSorted = clazzInfoList.sortedByDescending { it.fullyQualifiedName.length }

        javaFileTree.forEach { file ->
            doObfuscate(file, clazzInfoListSorted)
        }
    }

    private fun doObfuscate(file: File, clazzInfoList: List<ClazzInfo>) {
        var fileContent = file.readText()
        println()
        println("replacing file $file ...")

        clazzInfoList.forEach { clazzinfo ->
            //replace self class name
            if (clazzinfo.isBelongThisFile(file)) {
                fileContent = fileContent.replaceWords(
                    clazzinfo.rawClazzName,
                    clazzinfo.obfuscateClazzName
                )
            }

            //current file imports
            val fileImports =
                clazzInfoList.find { it.isBelongThisFile(file) }?.imports ?: emptyList()

            //list this file may import
            val mayImportsModality = mutableListOf<String>()
            val clazzLayerNames =
                clazzinfo.fullyQualifiedName.replace("${clazzinfo.packageName}.", "").split(".")
            for (layer in clazzLayerNames.size downTo 1) {
                val assumeModality =
                    clazzinfo.packageName + "." + clazzLayerNames.take(layer).joinToString(".")
                mayImportsModality.add(assumeModality)
            }

            println("current checking clazzinfo -> ${clazzinfo.fullyQualifiedName}")
            println("import maybe $mayImportsModality")

            val needTryReplaceQuotePairs = mutableListOf<Pair<String, String>>()
            if (
                fileImports.hasOneOf(mayImportsModality) { s, s1 -> s == s1 }
                || file.packagePath() == clazzinfo.packageName
                //import * maybe not stable in some cases
                || fileImports.contains("${clazzinfo.packageName}.*")
            ) {
                //should replace with a string that fully qualified name delete package name
                val raw = clazzinfo.fullyQualifiedName.replace("${clazzinfo.packageName}.", "")
                val obfuscate =
                    clazzinfo.fullyObfuscateQualifiedName.replace("${clazzinfo.packageName}.", "")
                println("replace $raw to $obfuscate")
                val classLayerCount = raw.split(".").size
                println("classLayerCount = $classLayerCount")
                for (layer in classLayerCount downTo 1) {
                    val oldValue = raw.split(".").takeLast(layer).joinToString(".")
                    val newValue = obfuscate.split(".").takeLast(layer).joinToString(".")
                    println("file -> $file do replace $oldValue to $newValue")
                    needTryReplaceQuotePairs.add(Pair(oldValue, newValue))
                }
            }

            needTryReplaceQuotePairs.sortByDescending { it.first.length }

            //do replace
            for (replacement in needTryReplaceQuotePairs) {
                fileContent = fileContent.replaceWords(replacement.first, replacement.second)
            }

            //replace imports
            fileContent = fileContent
                .replaceWords(
                    "import ${clazzinfo.packageName}.${clazzinfo.rawClazzName};",
                    "import ${clazzinfo.packageName}.${clazzinfo.obfuscateClazzName};"
                )
                .replaceWords(
                    "import ${clazzinfo.fullyQualifiedName};",
                    "import ${clazzinfo.fullyObfuscateQualifiedName};"
                )
                .replaceWords(clazzinfo.fullyQualifiedName, clazzinfo.fullyObfuscateQualifiedName)
        }

        //find file obfuscate name
        val fileObfuscateName = clazzInfoList.find {
            it.isBelongThisFile(file)
        }?.belongFileObfuscateName ?: return
        //rename file
        val obfuscatePath =
            file.parentFile.absolutePath + File.separator + fileObfuscateName + javaFileExtensionName
        val newFile = File(obfuscatePath)
        newFile.writeText(fileContent)
        //delete old file
        file.delete()
    }

    private fun updateLayoutXml() {
        val layoutDirs = project.findLayoutDirs(variantName)
        val layoutDirFileTree = project.files(layoutDirs).asFileTree

        layoutDirFileTree.forEach {
            var content = it.readText()
            clazzInfoList.forEach { info ->
                val raw = info.packageName + "." + info.rawClazzName
                //eg: com.littlew.example.pc.ATxtCusView
                val obfuscate = info.packageName + "." + info.obfuscateClazzName
                content = content.replaceWords(raw, obfuscate)
            }
            it.writeText(content)
        }
    }

    private fun updateManifest() {
        val manifest = project.manifestFile()
        var manifestContent = manifest.readText()
        //R path
        val packagename = project.findPackageName()

        clazzInfoList.forEach { info ->
            val raw = info.packageName + "." + info.rawClazzName
            val obfuscate = info.packageName + "." + info.obfuscateClazzName
            manifestContent = manifestContent
                //eg: .MainActivity
                .replaceWords(raw, obfuscate)
                //eg: com.littlew.example.SecondActivity
                .replaceWords(raw.replace(packagename, ""), obfuscate.replace(packagename, ""))
        }
        manifest.writeText(manifestContent)
    }

}