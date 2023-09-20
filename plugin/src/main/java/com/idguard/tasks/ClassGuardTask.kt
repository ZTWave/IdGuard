package com.idguard.tasks

import com.idguard.modal.ClazzInfo
import com.idguard.modal.ConstructorInfo
import com.idguard.modal.MethodInfo
import com.idguard.utils.*
import com.idguard.writer.ObfuscateModelWriter2
import com.idguard.writer.ObfuscateWriter
import com.thoughtworks.qdox.JavaProjectBuilder
import com.thoughtworks.qdox.model.JavaClass
import com.thoughtworks.qdox.model.JavaSource
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject


abstract class ClassGuardTask @Inject constructor(
    private val variantName: String,
    private val whiteList: List<String>
) : DefaultTask() {

    init {
        group = "guard"
    }

    private val javaFileExtensionName = ".java"
    private val clazzInfoList = mutableListOf<ClazzInfo>()
    private val mappingName = "class_guard_mapping.txt"

    private fun modelWriter(clazzInfos: List<ClazzInfo>): ObfuscateWriter {
        return ObfuscateModelWriter2().apply {
            this.projectClazzInfos = clazzInfos
        }
    }

    /**
     * only for java source file
     */
    @TaskAction
    fun execute() {
        val javaFile = mutableListOf<File>()
        project.rootProject.subprojects {
            if (it.isAndroidProject()) {
                javaFile.addAll(it.javaDirs(variantName))
            }
        }
        val javaFilesTree = project.rootProject.files(javaFile).asFileTree

        val fileSourceMap = mutableMapOf<File, JavaSource>()

        val javaProjectBuilder = JavaProjectBuilder().apply {
            javaFilesTree.forEach { file ->
                println("add file $file")
                addSource(file).also {
                    fileSourceMap[file] = it
                }
            }
        }

        println("white list -> $whiteList")
        println("white list class will not be obfuscate but will update other obfuscate reference!")

        //this source to list will cause element repetition problem
        val sources = javaProjectBuilder.sources.toSet()

        val allClass = javaProjectBuilder.classes.toSet()

        println("start fill class info...")
        fillClazzInfo(allClass, fileSourceMap)
        println("class info analyze finished.")

        println("start find class extend and implements node...")
        relatedClazzNodes(allClass)
        println("class info extend and implement analyze finished.")

        println("start fill obfuscate info...")
        fillClazzObfuscateInfo(javaFilesTree)
        println("fill obfuscate info finished")

        println("start fill override fun obfuscate name...")
        //find no obfuscate override method name and fill it
        fillMethodObInfo()
        println("fill override fun obfuscate name finished.")

        println("start fill obfuscated fully qualified name...")
        fillObfuscateFullyQualifiedName()
        println("fill obfuscated fully qualified name finished.")

        println("start obfuscate...")
        obfuscateJavaFile(sources)

        //manifest 内容替换
        updateManifest()

        //替换layout中的自定义view
        updateLayoutXml()

        outputMapping()
    }

    private fun fillClazzObfuscateInfo(javaSourceFileTree: FileTree) {
        //ensure class Name first letter is Uppercase
        clazzInfoList.forEach {
            ClazzInfoObfuscate.fillClassNameObfuscateInfo(it, inWhiteList(it))
        }
        clazzInfoList.forEach {
            ClazzInfoObfuscate.fillClassContentObfuscateInfo(it, inWhiteList(it))
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

    private fun outputMapping() {
        MappingOutputHelper.clearText(project, mappingName)

        clazzInfoList.forEach { clazzinfo ->
            MappingOutputHelper.appendNewLan(
                project,
                mappingName,
                "${clazzinfo.fullyQualifiedName} -> ${clazzinfo.fullyObfuscateQualifiedName}"
            )

            MappingOutputHelper.appendNewLan(
                project,
                mappingName,
                "constructors"
            )
            clazzinfo.constructors.forEach { constructorInfo: ConstructorInfo ->
                MappingOutputHelper.appendNewLan(
                    project,
                    mappingName,
                    "\t ${clazzinfo.rawClazzName} -> ${clazzinfo.obfuscateClazzName}"
                )
                constructorInfo.params.forEach { params ->
                    MappingOutputHelper.appendNewLan(
                        project,
                        mappingName,
                        "\t\t param ${params.rawFullyQualifiedName} ${params.rawName} -> ${params.obfuscateName}"
                    )
                }
            }

            MappingOutputHelper.appendNewLan(
                project,
                mappingName,
                "fields"
            )
            clazzinfo.fieldList.forEach { fieldInfo ->
                MappingOutputHelper.appendNewLan(
                    project,
                    mappingName,
                    "\t ${fieldInfo.rawName} -> ${fieldInfo.obfuscateName}"
                )
            }

            MappingOutputHelper.appendNewLan(
                project,
                mappingName,
                "methods"
            )
            clazzinfo.methodList.forEach { methodInfo ->
                MappingOutputHelper.appendNewLan(
                    project,
                    mappingName,
                    "\t ${methodInfo.rawName} -> ${methodInfo.obfuscateName}"
                )
                methodInfo.params.forEach { params ->
                    MappingOutputHelper.appendNewLan(
                        project,
                        mappingName,
                        "\t\t param ${params.rawFullyQualifiedName} ${params.rawName} -> ${params.obfuscateName}"
                    )
                }
            }

            MappingOutputHelper.appendNewLan(
                project,
                mappingName,
                ""
            )
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

    private fun fillClazzInfo(
        classList: Set<JavaClass>,
        fileClassMap: MutableMap<File, JavaSource>
    ) {
        classList.forEach { javaClass ->
            val clazzInfo = javaClass.parser()

            println("start find class ${clazzInfo.fullyQualifiedName} belong file")

            var sourceFile: File? = null

            run loopFor@{
                fileClassMap.forEach { (file, javaSource) ->
                    if (findJavaSourceWithJavaClass(javaSource, clazzInfo)) {
                        sourceFile = file
                        return@loopFor
                    }
                }
            }

            println("finish find ${clazzInfo.fullyQualifiedName} file -> $sourceFile")

            if (sourceFile == null) {
                throw RuntimeException("can't find this class ${javaClass.packageName} ${javaClass.name} belong file")
            }

            clazzInfo.belongFile = sourceFile
            clazzInfoList.add(clazzInfo)

            if (clazzInfo.belongFile == null) {
                println("error find class in all files -> ${clazzInfo.rawClazzName}")
            }
        }
    }

    private fun findJavaSourceWithJavaClass(
        javaSource: JavaSource,
        clazzInfo: ClazzInfo
    ): Boolean {
        val classes = javaSource.classes.toList()
        if (classes.find { clazzInfo.isCorrespondingJavaClass(it) } != null) {
            return true
        }

        classes.forEach { javaClass ->
            if (findNestedCorrespondingClass(javaClass, clazzInfo)) {
                return true
            }
        }

        return false
    }

    private fun findNestedCorrespondingClass(javaClass: JavaClass, clazzInfo: ClazzInfo): Boolean {
        val allClass = mutableListOf<JavaClass>()
        findAllNestedClass(javaClass, allClass)

        allClass.forEach {
            if (clazzInfo.isCorrespondingJavaClass(it)) {
                return true
            }
        }

        return false
    }

    private fun findAllNestedClass(javaClass: JavaClass, allClass: MutableList<JavaClass>) {
        val layer = mutableListOf<JavaClass>()
        layer.addAll(javaClass.nestedClasses)
        if (layer.isEmpty()) {
            return
        }
        allClass.addAll(layer)
        layer.forEach {
            findAllNestedClass(it, allClass)
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
            println("base -> ${clazzInfo.packageName}.${clazzInfo.rawClazzName} parent nodes method except override -> ${parentMethods.map { it.rawName }}")
            clazzInfo.methodList.forEach { method ->
                val find = parentMethods.find { it.isSameFun(method) }
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
    private fun relatedClazzNodes(classes: Set<JavaClass>) {
        //establishing extend and implements node relation
        classes.forEach { javaClass ->
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

    private fun obfuscateJavaFile(sources: Set<JavaSource>) {
        val obFileNameSourceMap = mutableMapOf<String, String>()

        sources.forEachIndexed { _, javaSource ->
            val writer = modelWriter(clazzInfoList)

            val belongFile = findBelongFileForSource(javaSource)
            val clazzInfo = clazzInfoList.find { it.isBelongThisFile(belongFile) }
                ?: throw RuntimeException("can't find exception for file $belongFile")

            val newFile =
                belongFile.parentFile.absolutePath + File.separator + clazzInfo.belongFileObfuscateName + javaFileExtensionName

            writer.writeSource(belongFile, javaSource)
            obFileNameSourceMap[newFile] = writer.toString()

            //delete original file
            belongFile.delete()
        }

        obFileNameSourceMap.forEach { (newFile, source) ->
            File(newFile).writeText(source)
        }
    }

    private fun inWhiteList(clazzInfo: ClazzInfo): Boolean {
        val belongFile = clazzInfo.belongFile ?: return false
        val belongFilePackageName = clazzInfo.packageName + "." + belongFile.name
        val isJavaFileInWhiteList = whiteList.contains(belongFilePackageName)
        if (isJavaFileInWhiteList) {
            //end with .java
            println("file name -> $belongFilePackageName is white -> $isJavaFileInWhiteList")
            return true
        }

        val recursionPackage = mutableListOf<String>()
        val packageNames = belongFilePackageName.split(".")
        for (layerIndex in packageNames.size downTo 1) {
            recursionPackage.add(packageNames.take(layerIndex).joinToString("."))
        }
        val packageInWhiteList = recursionPackage.hasOneOf(whiteList, equalBlock = { s, s2 ->
            s == s2
        })
        if (packageInWhiteList) {
            println("source -> ${clazzInfo.packageName} is white -> $packageInWhiteList")
            return true
        }
        return false
    }

    private fun findBelongFileForSource(javaSource: JavaSource): File {
        val oneClazzInfo = javaSource.classes.firstOrNull()
            ?: throw RuntimeException("this source has no java source")
        val clazzInfo = clazzInfoList.find { it.isCorrespondingJavaClass(oneClazzInfo) }
        return clazzInfo?.belongFile
            ?: throw RuntimeException("this class info is null or belong file is null")
    }

    private fun updateLayoutXml() {

        val layoutDirs = mutableListOf<File>()

        project.rootProject.subprojects {
            if (it.isAndroidProject()) {
                layoutDirs.addAll(it.findLayoutDirs(variantName))
            }
        }

        val layoutDirFileTree = project.rootProject.files(layoutDirs).asFileTree

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
        project.rootProject.subprojects { project ->
            if (project.isAndroidProject()) {

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
                        .replaceWords(
                            raw.replace(packagename, ""),
                            obfuscate.replace(packagename, "")
                        )
                }

                manifest.writeText(manifestContent)
            }
        }
    }

}