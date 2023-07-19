package com.idguard

import com.idguard.modal.ClazzInfo
import com.idguard.utils.MappingOutputHelper
import com.idguard.utils.findLayoutDirs
import com.idguard.utils.findPackageName
import com.idguard.utils.javaDirs
import com.idguard.utils.manifestFile
import com.idguard.utils.replaceWords
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

    private val fileNameMap = mutableMapOf<String, ClazzInfo>()
    private val mappingName = "class_guard_mapping.text"

    /**
     * only for java source file
     */
    @TaskAction
    fun execute() {
        val javaFile = project.javaDirs(variantName)
        val javaFileTree = project.files(javaFile).asFileTree
        javaFileTree.forEach {
            fileNameMap[it.absolutePath] = ClazzInfo(it)
        }

        //java file rename and re-import
        obfuscateJavaFile(javaFileTree)

        //manifest 内容替换
        updateManifest()

        //替换layout中的自定义view
        updateLayoutXml()

        outputMapping()
    }

    private fun outputMapping() {
        val outputMap = fileNameMap.map {
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
                fileNameMap[file.absolutePath] ?: throw RuntimeException("file tree has changed")
            var fileContent = file.readText()
            fileNameMap.forEach nameForEach@{ (filePath, clazzinfo) ->
                if (!clazzInfo.isThisClazzImported(file)) {
                    return@nameForEach
                }
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
            fileNameMap.forEach { (filePath, info) ->
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

        fileNameMap.forEach { (filePath, info) ->
            val raw = info.packageName + "." + info.rawClazzName
            val obfuscate = info.packageName + "." + info.obfuscateClazzName
            manifestContent = manifestContent.replaceWords(raw, obfuscate)
                .replaceWords(raw.replace(packagename, ""), obfuscate.replace(packagename, ""))
        }
        manifest.writeText(manifestContent)
    }

}