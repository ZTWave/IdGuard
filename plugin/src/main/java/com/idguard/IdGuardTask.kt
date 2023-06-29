package com.idguard

import com.android.build.gradle.BaseExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject
import kotlin.random.Random

open class IdGuardTask @Inject constructor(
    private val variantName: String,
) : DefaultTask() {

    init {
        group = "guard"
    }

    //key raw ,value obfuscate
    private val layoutROMap = mutableMapOf<String, String>()

    @TaskAction
    fun execute() {
        val layoutDirs = mutableListOf<File>()
        project.rootProject.subprojects {
            if (it.isAndroidProject()) {
                layoutDirs.addAll(it.findLayoutDirs(variantName))
            }
        }
        println(layoutDirs)

        val layoutDirFileTree = project.files(layoutDirs).asFileTree
        //混淆map生成
        layoutDirFileTree.forEach { file ->
            println(file)
            val layoutFileName = file.name
            println("file name $layoutFileName")
            val fileParentPath = file.parentFile.absolutePath
            val obfuscateName = genLayoutFileObfuscateName()
            layoutROMap[file.absolutePath] =
                fileParentPath + File.separator + obfuscateName + ".xml"
        }
        println(layoutROMap)
        //混淆layout xml
        layoutDirFileTree.forEach { file ->
            val fileText = file.readText()
            layoutROMap.forEach { raw, obfuscate ->
                val rawName = raw.getFileName()
                val obfuscateName = obfuscate.getFileName()
                fileText.replaceWords("@layout/$rawName", "@layout/$obfuscateName")
            }
            file.writeText(fileText)
        }

    }

    /**
     * 通过文件的全路径获取文件名称
     */
    fun String.getFileName(): String {
        val name = this.split(File.separator).lastOrNull()
            ?: throw RuntimeException("file path is error $this")
        return name.split('.')[1]
    }

    fun Project.findLayoutDirs(variantName: String) = findXmlDirs(variantName, "layout")
    fun Project.findXmlDirs(variantName: String, vararg dirName: String): ArrayList<File> {
        return resDirs(variantName).flatMapTo(ArrayList()) { dir ->
            dir.listFiles { file, name ->
                //过滤res目录下xxx目录
                file.isDirectory && dirName.any { name.startsWith(it) }
            }?.toList() ?: emptyList()
        }
    }

    //返回res目录,可能有多个
    fun Project.resDirs(variantName: String): List<File> {
        println("$project")
        println("extensions ${project.extensions}")
        val sourceSet = (project.extensions.getByName("android") as BaseExtension).sourceSets
        val nameSet = mutableSetOf<String>()
        nameSet.add("main")
        if (isAndroidProject()) {
            nameSet.addAll(variantName.splitWords())
        }
        val resDirs = mutableListOf<File>()
        sourceSet.names.forEach { name ->
            if (nameSet.contains(name)) {
                sourceSet.getByName(name).res.srcDirs.mapNotNullTo(resDirs) {
                    if (it.exists()) it else null
                }
            }
        }
        return resDirs
    }

    fun Project.isAndroidProject() =
        plugins.hasPlugin("com.android.application")
            || plugins.hasPlugin("com.android.library")

    private fun String.splitWords(): List<String> {
        val regex = Regex("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])")
        return split(regex).map { it.lowercase() }
    }

    private val numberAndLine = "1234567890_"
    private val charsLatter = "abcdefghijklmnopqrstuvwxyz"
    private val charsUpper = charsLatter.uppercase()

    private fun dic() = run {
        val dicSb = StringBuilder(charsLatter)
        dicSb.append(charsUpper)
        dicSb.append(numberAndLine)
        dicSb.toString()
    }

    fun genLayoutFileObfuscateName(): String {
        val nameLengthPair = Pair(4, 9)
        val dic = dic()
        val rNameLength = Random.nextInt(nameLengthPair.first, nameLengthPair.second + 1)
        val nameSb = StringBuilder("")
        while (nameSb.length < rNameLength) {
            val cIndex = Random.nextInt(0, dic.length + 1)
            val c = dic[cIndex]
            if (nameSb.isEmpty() && numberAndLine.contains(c)) {
                continue
            }
            nameSb.append(dic.get(cIndex))
        }
        return nameSb.toString()
    }

    fun String.replaceWords(
        oldValue: String,
        newValue: String,
        ignoreCase: Boolean = false
    ): String {
        var occurrenceIndex: Int = indexOf(oldValue, 0, ignoreCase)
        // FAST PATH: no match
        if (occurrenceIndex < 0) return this

        val oldValueLength = oldValue.length
        val searchStep = oldValueLength.coerceAtLeast(1)
        val newLengthHint = length - oldValueLength + newValue.length
        if (newLengthHint < 0) throw OutOfMemoryError()
        val stringBuilder = StringBuilder(newLengthHint)

        var i = 0
        do {
            if (isWord(occurrenceIndex, oldValue)) {
                stringBuilder.append(this, i, occurrenceIndex).append(newValue)
            } else {
                stringBuilder.append(this, i, occurrenceIndex + oldValueLength)
            }
            i = occurrenceIndex + oldValueLength
            if (occurrenceIndex >= length) break
            occurrenceIndex = indexOf(oldValue, occurrenceIndex + searchStep, ignoreCase)
        } while (occurrenceIndex > 0)
        return stringBuilder.append(this, i, length).toString()
    }

    fun String.isWord(index: Int, oldValue: String): Boolean {
        val firstChar = oldValue[0].code
        if (index > 0 && (firstChar in 65..90 || firstChar == 95 || firstChar in 97..122)) {
            val prefix = get(index - 1).code
            // $ . 0-9 A-Z _ a-z
            if (prefix == 36 || prefix == 46 || prefix in 48..57 || prefix in 65..90 || prefix == 95 || prefix in 97..122) {
                return false
            }
        }
        val endChar = oldValue[oldValue.lastIndex].code
        // $ 0-9 A-Z _ a-z
        if (endChar == 36 || endChar in 48..57 || endChar in 65..90 || endChar == 95 || endChar in 97..122) {

            val suffix = getOrNull(index + oldValue.length)?.code
            // $ 0-9 A-Z _ a-z
            if (suffix == 36 || suffix in 48..57 || suffix in 65..90 || suffix == 95 || suffix in 97..122) {
                return false
            }
        }
        return true
    }
}