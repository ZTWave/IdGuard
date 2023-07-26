package com.idguard

import com.idguard.modal.ClazzInfo
import com.idguard.modal.MethodInfo
import com.idguard.utils.javaDirs
import com.idguard.utils.parser
import com.thoughtworks.qdox.JavaProjectBuilder
import org.gradle.api.DefaultTask
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

    private val clazzInfoList = mutableListOf<ClazzInfo>()
    private val funNameMapping = mutableMapOf<String, String>()

    private val mappingName = "class_guard_mapping.text"

    /**
     * only for java source file
     */
    @TaskAction
    fun execute() {
        val javaFile = project.javaDirs(variantName)
        println("src path -> $javaSrcPath")
        val javaSrc = File(javaSrcPath)
        if (!javaSrc.exists()) {
            throw RuntimeException("java src -> $javaFile is not exits")
        }
        val javaProjectBuilder = JavaProjectBuilder().apply {
            addSourceTree(javaSrc)
        }
        println("class size -> ${javaProjectBuilder.classes.size} module size -> ${javaProjectBuilder.modules.size}")
        javaProjectBuilder.classes.forEach {
            clazzInfoList.add(it.parser())
        }
        println("class info parent nested class analyze finished.")

        println("start find class extend and implements node...")
        relatedClazzNodes(javaProjectBuilder)
        println("class info extend and implement analyze finished.")

        println("start fill override fun obfuscate name...")
        //find no obfuscate override method name
        fillMethodObInfo()
        println("fill override fun obfuscate name finished.")

//        println("start obfuscate...")
        //startObfuscateFile()


        clazzInfoList.forEach {
            println("${it.modifier} ${it.rawClazzName} extend ${it.extendNode?.fullyQualifiedName} implements ${it.implNodes.map { node -> node.fullyQualifiedName }}")
            println("parent -> ${it.parentNode?.rawClazzName ?: "null"} ")
            println("fullyQualifiedName -> ${it.fullyQualifiedName}")
            val methodPrint = it.methodList.map { me -> "${me.name} -> ${me.obfuscateName}" }
            println("method -> $methodPrint")
            val fieldsPrint =
                it.fieldList.map { field -> "${field.name} -> ${field.obfuscateName}" }
            println("fields -> $fieldsPrint")
            println(it.bodyInfo)
            println()
        }

        //FIXME open this comment

        //java file rename and re-import
        //obfuscateJavaFile(javaFileTree)

        //manifest 内容替换
        //updateManifest()

        //替换layout中的自定义view
        //updateLayoutXml()

        //outputMapping()
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

        println("base -> ${rootNode.packageName}.${rootNode.rawClazzName} nodes -> ${nodes.map { it.rawClazzName }}")
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
            val node = clazzInfoList.find { it.isCorrespondingJavaClass(javaClass) } ?: return@forEach

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
    }

    private fun obfuscateJavaFile(javaFileTree: FileTree) {
        javaFileTree.forEach { file ->
            val clazzInfo =
                clazzInfoList[file.absolutePath] ?: throw RuntimeException("file tree has changed")
            var fileContent = file.readText()
            clazzInfoList.forEach nameForEach@{ (filePath, clazzinfo) ->
                fileContent = fileContent
                    .replaceWords(clazzinfo.rawClazzName, clazzinfo.obfuscateClazzName)
                    .replaceWords(
                        "import ${clazzinfo.packageName}.${clazzinfo.rawClazzName}",
                        "import ${clazzinfo.packageName}.${clazzinfo.obfuscateClazzName}"
                    )
            }
            val newFile = File(clazzInfo.obfuscatePath)
            newFile.writeText(fileContent)
            file.delete()
        }
    }

    private fun updateLayoutXml() {
        val layoutDirs = project.findLayoutDirs(variantName)
        val layoutDirFileTree = project.files(layoutDirs).asFileTree

        layoutDirFileTree.forEach {
            var content = it.readText()
            clazzInfoList.forEach { (filePath, info) ->
                val raw = info.packageName + "." + info.rawClazzName
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

        clazzInfoList.forEach { (filePath, info) ->
            val raw = info.packageName + "." + info.rawClazzName
            val obfuscate = info.packageName + "." + info.obfuscateClazzName
            manifestContent = manifestContent.replaceWords(raw, obfuscate)
                .replaceWords(raw.replace(packagename, ""), obfuscate.replace(packagename, ""))
        }
        manifest.writeText(manifestContent)
    }*/

}