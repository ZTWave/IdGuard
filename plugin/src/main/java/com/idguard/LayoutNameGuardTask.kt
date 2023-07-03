package com.idguard

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject
import kotlin.random.Random

open class LayoutNameGuardTask @Inject constructor(
    private val variantName: String,
) : DefaultTask() {

    init {
        group = "guard"
    }


    private val numberAndLine = "1234567890_"
    private val charsLatter = "abcdefghijklmnopqrstuvwxyz"
    private val charsUpper = charsLatter.uppercase()

    //key raw ,value obfuscate
    private val layoutROMap = mutableMapOf<String, String>()

    //name min and max length
    private val nameLengthPair = Pair(4, 9)

    @TaskAction
    fun execute() {
        val layoutDirs = mutableListOf<File>()
        project.rootProject.subprojects {
            if (it.isAndroidProject()) {
                layoutDirs.addAll(it.findLayoutDirs(variantName))
            }
        }
        //println(layoutDirs)

        val layoutDirFileTree = project.files(layoutDirs).asFileTree
        val allObsNames = genAllLayoutFileObfuscateName(layoutDirFileTree.files.size)
        //混淆map生成
        layoutDirFileTree.forEachIndexed { index, file ->
            println(file)
            val layoutFileName = file.name
            println("file name $layoutFileName")
            val fileParentPath = file.parentFile.absolutePath
            val obfuscateName = allObsNames[index]
            layoutROMap[file.absolutePath] =
                fileParentPath + File.separator + obfuscateName + ".xml"
        }
        //println(layoutROMap)

        //混淆layout xml里的layout引用
        val needReplaceResFile = mutableListOf<File>()
        project.rootProject.subprojects {
            if (it.isAndroidProject()) {
                needReplaceResFile.addAll(it.findLayoutUsagesInRes(variantName))
            }
        }
        val needReplaceResFileTree = project.files(needReplaceResFile).asFileTree
        needReplaceResFileTree.forEach { file ->
            var fileText = file.readText()
            layoutROMap.forEach { (raw, obfuscate) ->
                val rawName = raw.getFileName()
                val obfuscateName = obfuscate.getFileName()
//                println("$rawName -> $obfuscateName")
                //@layout/activity_main
                fileText = fileText.replaceWords("@layout/$rawName", "@layout/$obfuscateName")
            }
//            println()
//            println(file.name)
//            println(fileText)
//            println()
            file.writeText(fileText)
        }
        //混淆layout名称
        val layoutFiles = layoutDirFileTree.files.toSet()
        layoutFiles.forEach { file ->
            val obfuscateFilePath = layoutROMap[file.absolutePath]
                ?: throw RuntimeException("layout dir has changed !!!")
            val obfuscateFile = File(obfuscateFilePath)
            obfuscateFile.writeText(file.readText())
            file.delete()
        }
        //混淆java 或者 kotlin文件对layout引用
        val javaFileTree = project.javaDirs(variantName)
        project.files(javaFileTree).asFileTree.forEach { javaFile ->
            var javaFileText = javaFile.readText()
            layoutROMap.forEach { (raw, obfuscate) ->
                val rawName = raw.getFileName()
                val obfuscateName = obfuscate.getFileName()
                javaFileText =
                    javaFileText.replaceWords("R.layout.$rawName", "R.layout.$obfuscateName")
            }
            javaFile.writeText(javaFileText)
        }
    }

    //生成对应个数的随机名称
    private fun genAllLayoutFileObfuscateName(size: Int): List<String> {
        val nameSet = mutableSetOf<String>()
        var count = 0
        while (nameSet.size < size) {
            nameSet.add(genLayoutFileObfuscateName())
            count++
        }
        println("gen $size random names, used $count times loop")
        return nameSet.toList()
    }

    private fun dic() = run {
        val dicSb = StringBuilder(charsLatter)
        dicSb.append(charsUpper)
        dicSb.append(numberAndLine)
        dicSb.toString()
    }

    private fun genLayoutFileObfuscateName(): String {
        val dic = dic()
        val rNameLength = Random.nextInt(nameLengthPair.first, nameLengthPair.second + 1)
        val nameSb = StringBuilder("")
        while (nameSb.length < rNameLength) {
            val cIndex = Random.nextInt(0, dic.length)
            val c = dic[cIndex]
            if (nameSb.isEmpty() && numberAndLine.contains(c)) {
                continue
            }
            nameSb.append(dic[cIndex])
        }
        return nameSb.toString()
    }
}