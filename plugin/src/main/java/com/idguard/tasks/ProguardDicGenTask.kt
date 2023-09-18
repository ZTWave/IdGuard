package com.idguard.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.inject.Inject
import kotlin.random.Random

open class ProguardDicGenTask @Inject constructor(
    private val dictCapacity: Int
) : DefaultTask() {

    init {
        group = "guard"
    }

    private val charDic = mutableListOf<String>()

    private val dictFileName = "dict.txt"

    private val nameLength = Pair(2, 10)

    @TaskAction
    fun execute() {
        initDic()
        val dictFile = createDictFile()
        writeContent(dictFile)
    }

    private fun writeContent(dictFile: File) {
        val nameSet = mutableSetOf<String>()
        val secureRandom = SecureRandom()
        do {
            val seed = secureRandom.generateSeed(10)
            val byteBuffer = ByteBuffer.allocate(10)
            byteBuffer.put(seed)
            byteBuffer.flip()
            val seedLong = byteBuffer.long
            val nameLength = Random(seedLong).nextInt(nameLength.first, nameLength.second + 1)

            var name = ""
            repeat(nameLength) {
                val c = charDic[secureRandom.nextInt(charDic.size)]
                name += c
            }

            nameSet.add(name)

        } while (nameSet.size < dictCapacity)

        val bufferWriter = dictFile.bufferedWriter()
        nameSet.forEachIndexed { index, name ->
            bufferWriter.write(name)
            if (index < nameSet.size - 1) {
                bufferWriter.newLine()
            }
            bufferWriter.flush()
        }
        bufferWriter.close()

        println("please add the follow lines in your proguard file")
        println("-obfuscationdictionary $dictFileName")
        println("-classobfuscationdictionary $dictFileName")
        println("-packageobfuscationdictionary $dictFileName")
        println("")
    }

    private fun createDictFile(): File {
        val dictFile = project.file(dictFileName)
        if (dictFile.exists()) {
            dictFile.writeText("")
        } else {
            dictFile.createNewFile()
        }
        return dictFile
    }

    private fun initDic() {
        val number = (0..9).toList().map { it.toString() }
        val charLetter = ('a'..'z').map { it.toString() }
        val charUpper = ('A'..'Z').map { it.toString() }
        val underline = "_"
        charDic.addAll(number)
        charDic.addAll(charLetter)
        charDic.addAll(charUpper)
        charDic.add(underline)
    }

}
