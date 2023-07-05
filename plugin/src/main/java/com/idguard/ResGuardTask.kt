package com.idguard

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

open class ResGuardTask @Inject constructor(
    private val variantName: String,
) : DefaultTask() {

    init {
        group = "guard"
    }

    private val drawableNameMap = mutableMapOf<String, String>()

    //哪些扩展名的文件内容需要替换
    private val needReplaceFileExtensionName = listOf(".xml", ".java", ".kt")

    private val mappingName = "res_guard_mapping.text"

    @TaskAction
    fun execute() {
        val drawableFileTree = project.files(findNeedSearchFiles()).asFileTree
//        println("drawable files")
//        println(drawableFiles)
        val drawableNameSet = mutableSetOf<String>()
        drawableFileTree.forEach {
            val rawName = it.getRealName()
            drawableNameSet.add(rawName)
        }
//        println("drawableNameSet")
//        println(drawableNameSet)
        val drawableObfuscateNames =
            RandomNameHelper.genNames(drawableNameSet.size, Pair(6, 10), allLetter = true)
//        println("drawableObfuscateNameSet")
//        println(drawableObfuscateNameSet)
        drawableNameMap.putAll(drawableNameSet.mapIndexed { index: Int, name: String ->
            name to drawableObfuscateNames[index]
        })
//        println("drawableNameMap")
//        println(drawableNameMap)
        val needReplaceFiles = findNeedReplaceFiles()
        project.files(needReplaceFiles).asFileTree.forEach {
            if (!needReplaceFileExtensionName.contains(it.getExtensionName())) {
                //如果不加这个剔除功能 可能会对某些文件有影响
                return@forEach
            }
            var text = it.readText()
            drawableNameMap.forEach { (raw, obfuscate) ->
                text = text.replaceWords("R.drawable.$raw", "R.drawable.$obfuscate")
                    .replaceWords("@drawable/$raw", "@drawable/$obfuscate")
                    .replaceWords("R.mipmap.$raw", "R.mipmap.$obfuscate")
                    .replaceWords("@mipmap/$raw", "@mipmap/$obfuscate")
            }
            it.writeText(text)
        }
        drawableFileTree.forEach {
            val obfuscateFilePath =
                it.parent + File.separator + drawableNameMap[it.getRealName()]!! + it.getExtensionName()
            it.renameTo(File(obfuscateFilePath))
        }

        MappingOutputHelper.write(project, mappingName, drawableNameMap)
    }

    private fun findNeedSearchFiles(): List<File> {
        val resDirs = mutableListOf<File>()
        project.rootProject.subprojects {
            if (it.isAndroidProject()) {
                resDirs.addAll(it.findDirsInRes(variantName, "drawable", "mipmap"))
            }
        }
        return resDirs.toList()
    }

    private fun findNeedReplaceFiles(): List<File> {
        val dirs = mutableListOf<File>()
        project.rootProject.subprojects {
            if (it.isAndroidProject()) {
                dirs.addAll(
                    it.findDirsInRes(
                        variantName,
                        "drawable",
                        "mipmap",
                        "values",
                        "layout",
                    )
                )
            }
        }
        dirs.addAll(project.javaDirs(variantName))
        dirs.add(project.manifestFile())
        return dirs.toList()
    }
}
