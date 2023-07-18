package com.idguard

import com.idguard.modal.ClazzInfo
import com.idguard.utils.MappingOutputHelper
import com.idguard.utils.findLayoutDirs
import com.idguard.utils.javaDirs
import com.idguard.utils.manifestFile
import com.idguard.utils.replaceWords
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

    private val fileNameMap = mutableMapOf<String, ClazzInfo>()
    private val manifestPackageRegex = Regex("package=\".+\"")
    private val buildGradleRegex = Regex("namespace .+")

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
        javaFileTree.forEach {
            val clazzInfo =
                fileNameMap[it.absolutePath] ?: throw RuntimeException("file tree has changed")

            var fileContent = it.readText()

            fileNameMap.forEach { (filePath, clazzinfo) ->
                fileContent =
                    fileContent.replaceWords(clazzinfo.rawClazzName, clazzinfo.obfuscateClazzName)
                        .replaceWords(
                            "import ${clazzinfo.packageName}.${clazzinfo.rawClazzName}",
                            "import ${clazzinfo.packageName}.${clazzinfo.obfuscateClazzName}"
                        )
            }

            val newFile = File(clazzInfo.obfuscatePath)
            newFile.writeText(fileContent)
            it.delete()
        }

        val manifest = project.manifestFile()
        var manifestContent = manifest.readText()

        //R path
        val packagename = findPackageName()

        fileNameMap.forEach { (filePath, info) ->
            val raw = info.packageName + "." + info.rawClazzName
            val obfuscate = info.packageName + "." + info.obfuscateClazzName
            manifestContent = manifestContent.replaceWords(raw, obfuscate)
                .replaceWords(raw.replace(packagename, ""), obfuscate.replace(packagename, ""))
        }

        manifest.writeText(manifestContent)

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

        val outputMap = fileNameMap.map {
            val info = it.value
            val raw = info.packageName + "." + info.rawClazzName
            val obfuscate = info.packageName + "." + info.obfuscateClazzName
            raw to obfuscate
        }.toMap()
        MappingOutputHelper.write(project, mappingName, outputMap)
    }

    private fun findPackageName(): String {
        val namespace = findNameSpaceInGradle()
        val packageName = findPackageInManifest()
        return if (namespace == "") {
            packageName
        } else {
            namespace
        }
    }

    private fun findPackageInManifest(): String {
        val manifest = project.manifestFile()
        val packageStr =
            manifestPackageRegex.find(manifest.readText())?.groupValues?.first() ?: return ""
        return packageStr.removePrefix("package=").removeSurrounding("\"")
    }

    private fun findNameSpaceInGradle(): String {
        val buildGradleFile = project.buildFile
        val namespaceStr =
            buildGradleRegex.find(buildGradleFile.readText())?.groupValues?.first() ?: return ""
        val namespace = namespaceStr.removePrefix("namespace").trim()
        return namespace.removeSurrounding("\'").removeSurrounding("\"")
    }

}