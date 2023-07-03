package com.idguard

import kotlin.random.Random

class RandomNameHelper {
    companion object {

        //name min and max length
        private val DefaultNameLengthPair = Pair(4, 9)

        private const val numberAndLine = "1234567890_"
        private const val charsLatter = "abcdefghijklmnopqrstuvwxyz"
        private val charsUpper = charsLatter.uppercase()

        private val dic = run {
            val dicSb = StringBuilder(charsLatter)
            dicSb.append(charsUpper)
            dicSb.append(numberAndLine)
            dicSb.toString()
        }

        //生成对应个数的随机名称
        fun genNames(size: Int, pair: Pair<Int, Int> = DefaultNameLengthPair): List<String> {
            val nameSet = mutableSetOf<String>()
            var count = 0
            while (nameSet.size < size) {
                nameSet.add(genAName(pair.first, pair.second))
                count++
            }
            println("gen $size random names, used $count times loop")
            return nameSet.toList()
        }

        private fun genAName(nameLengthMin: Int, nameLengthMax: Int): String {
            val rNameLength = Random.nextInt(nameLengthMin, nameLengthMax + 1)
            val nameSb = StringBuilder("")
            while (nameSb.length < rNameLength) {
                val c = if (nameSb.isEmpty()) {
                    val cIndex = Random.nextInt(0, charsLatter.length)
                    charsLatter[cIndex]
                } else {
                    val cIndex = Random.nextInt(0, dic.length)
                    dic[cIndex]
                }
                nameSb.append(c)
            }
            return nameSb.toString()
        }

    }
}