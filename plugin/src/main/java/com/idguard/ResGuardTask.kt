package com.idguard

import com.idguard.utils.MappingOutputHelper
import com.idguard.utils.RandomNameHelper
import com.idguard.utils.findDirsInRes
import com.idguard.utils.getExtensionName
import com.idguard.utils.getRealName
import com.idguard.utils.isAndroidProject
import com.idguard.utils.javaDirs
import com.idguard.utils.manifestFile
import com.idguard.utils.replaceWords
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
    private val stringNameMap = mutableMapOf<String, String>()

    private val strRegex = Regex("<(string|string-array) name=\"\\w+\">")

    //哪些扩展名的文件内容需要替换
    private val needReplaceFileExtensionName = listOf(".xml", ".java", ".kt")

    private val mappingName = "res_guard_mapping.text"

    @TaskAction
    fun execute() {
        // FIXME: remove this common
        drawableObfuscate()
        stringObfuscate()
        MappingOutputHelper.appendNewLan(project, mappingName, "drawable mapping")
        MappingOutputHelper.write(project, mappingName, drawableNameMap)
        MappingOutputHelper.appendNewLan(project, mappingName, "string mapping")
        MappingOutputHelper.write(project, mappingName, stringNameMap, append = true)
    }

    private fun drawableObfuscate() {
        val drawableFileTree = project.files(findNeedSearchFiles()).asFileTree
        val drawableNameSet = mutableSetOf<String>()
        drawableFileTree.forEach {
            val rawName = it.getRealName()
            drawableNameSet.add(rawName)
        }
        val drawableObfuscateNames =
            RandomNameHelper.genNames(drawableNameSet.size, Pair(6, 10), allLetter = true)
        drawableNameMap.putAll(drawableNameSet.mapIndexed { index: Int, name: String ->
            name to drawableObfuscateNames[index]
        })
        val needReplaceFiles = findNeedReplaceFiles(
            "drawable",
            "mipmap",
            "values",
            "layout",
        )
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
    }

    private fun stringObfuscate() {
        val needSearchFileTree = project.files(findStringsFiles()).asFileTree
        val namesSet = mutableSetOf<String>()
        needSearchFileTree.forEach {
            namesSet.addAll(findStringsName(it.readText()))
        }
        val obfuscateNames =
            RandomNameHelper.genNames(namesSet.size, Pair(8, 12), allLetter = false)
        stringNameMap.putAll(namesSet.mapIndexed { index: Int, name: String ->
            name to obfuscateNames[index]
        })
        val needReplaceFiles = findNeedReplaceFiles(
            "drawable",
            "values",
            "layout",
        )
        project.files(needReplaceFiles).asFileTree.forEach {
            if (!needReplaceFileExtensionName.contains(it.getExtensionName())) {
                //如果不加这个剔除功能 可能会对某些文件有影响
                return@forEach
            }
            var text = it.readText()
            stringNameMap.forEach { (raw, obfuscate) ->
                text = text.replaceWords("R.string.$raw", "R.string.$obfuscate")
                    .replaceWords("@string/$raw", "@string/$obfuscate")
                    .replaceWords("R.array.$raw", "R.array.$obfuscate")
                    .replaceWords("<string name=\"$raw\">", "<string name=\"$obfuscate\">")
                    .replaceWords(
                        "<string-array name=\"$raw\">",
                        "<string-array name=\"$obfuscate\">"
                    )
            }
            it.writeText(text)
        }
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

    /**
     * already contain java src dir and manifest file
     */
    private fun findNeedReplaceFiles(vararg dirNames: String): List<File> {
        val dirs = mutableListOf<File>()
        project.rootProject.subprojects {
            if (it.isAndroidProject()) {
                dirs.addAll(
                    it.findDirsInRes(
                        variantName,
                        *dirNames
                    )
                )
            }
        }
        dirs.addAll(project.javaDirs(variantName))
        dirs.add(project.manifestFile())
        return dirs.toList()
    }

    private fun findStringsFiles(): List<File> {
        val dirs = mutableListOf<File>()
        project.rootProject.subprojects {
            if (it.isAndroidProject()) {
                dirs.addAll(
                    it.findDirsInRes(
                        variantName,
                        "values",
                    )
                )
            }
        }
        return dirs.toList()
    }

    private fun findStringsName(stringText: String): List<String> {
        val result = strRegex.findAll(stringText)
        return result.map { it.value.removePrefix("<").removeSuffix(">").split("\"")[1] }.toList()
    }
}
