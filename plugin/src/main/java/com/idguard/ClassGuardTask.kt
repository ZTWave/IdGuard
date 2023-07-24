package com.idguard

import com.idguard.modal.ClazzInfo
import com.idguard.utils.javaDirs
import com.idguard.utils.parser
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

open class ClassGuardTask @Inject constructor(
    private val variantName: String,
) : DefaultTask() {

    init {
        group = "guard"
    }

    private val clazzInfoList = mutableListOf<ClazzInfo>()
    private val mappingName = "class_guard_mapping.text"

    /**
     * only for java source file
     */
    @TaskAction
    fun execute() {
        val javaFile = project.javaDirs(variantName)
        val javaFileTree = project.files(javaFile).asFileTree

        javaFileTree.forEach {
            clazzInfoList.addAll(it.parser())
        }
        println("class info parent nested class analyze finished.")
        println("start find class extend and implements node...")
        clazzInfoList.forEach { classInfo ->
            if (classInfo.extendName.isNotEmpty() || classInfo.implName.isNotEmpty()){

            }
        }

        clazzInfoList.forEach {
            println(it)
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