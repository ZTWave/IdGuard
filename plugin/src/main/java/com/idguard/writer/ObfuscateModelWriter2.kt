package com.idguard.writer

import com.idguard.modal.ClazzInfo
import com.idguard.utils.replaceStartWithDot
import com.idguard.utils.replaceWords
import com.thoughtworks.qdox.model.JavaSource
import java.io.File

/**
 * clazzinfos is project's
 *
 * use [writeSource] to write source in
 * use [toString] to get obfuscate source out
 */
class ObfuscateModelWriter2 : ObfuscateWriter {
    /**
     * 项目中的所有info 用于替换
     */
    var projectClazzInfos: List<ClazzInfo> = emptyList()

    private var fileContent: String = ""

    override fun writeSource(file: File, source: JavaSource) {
        fileContent = file.readText()

        replaceStaticMethod()

        replaceStaticField()

        replaceImport()

        replaceClassName()

        /*
        explain this line above will replace some field, method, import, enum usage with class name
        started.
        example :
        class A { public static final int a = 1;}
        class B { A.a; }
        will replace A.a to obfuscate code
        but if your code is just
        { a; }
        will not replace
        */

        //this will not replace field or method use .a or .a()
        replaceNoDotUsage()

        replaceDotUsage()
    }

    private fun replaceDotUsage() {
        val replacePair = mutableListOf<Pair<String, String>>()
        projectClazzInfos.forEach { clazzInfo ->
            val methods = clazzInfo.methodList
            val fields = clazzInfo.fieldList

            for (method in methods) {
                replacePair.add(
                    Pair(
                        method.rawName,
                        method.obfuscateName
                    )
                )
            }

            for (field in fields) {
                replacePair.add(
                    Pair(
                        field.rawName,
                        field.obfuscateName
                    )
                )
            }
        }

        contentReplaceNoWord(replacePair)
    }

    private fun replaceNoDotUsage() {
        val replacePair = mutableListOf<Pair<String, String>>()
        projectClazzInfos.forEach { clazzInfo ->
            val methods = clazzInfo.methodList
            val fields = clazzInfo.fieldList

            for (method in methods) {
                replacePair.add(Pair(method.rawName, method.obfuscateName))
                replacePair.add(
                    Pair(
                        "super.${method.rawName}",
                        "super.${method.obfuscateName}"
                    )
                )
                replacePair.add(
                    Pair(
                        "${clazzInfo.obfuscateClazzName}.super.${method.rawName}",
                        "${clazzInfo.obfuscateClazzName}.super.${method.obfuscateName}"
                    )
                )
                replacePair.add(
                    Pair(
                        "${clazzInfo.rawClazzName}.super.${method.rawName}",
                        "${clazzInfo.obfuscateClazzName}.super.${method.obfuscateName}"
                    )
                )
                replacePair.add(
                    Pair(
                        "this.${method.rawName}",
                        "this.${method.obfuscateName}"
                    )
                )
                replacePair.add(
                    Pair(
                        "${clazzInfo.obfuscateClazzName}.this.${method.rawName}",
                        "${clazzInfo.obfuscateClazzName}.this.${method.obfuscateName}"
                    )
                )
                replacePair.add(
                    Pair(
                        "${clazzInfo.rawClazzName}.this.${method.rawName}",
                        "${clazzInfo.obfuscateClazzName}.this.${method.obfuscateName}"
                    )
                )
            }
            for (field in fields) {
                replacePair.add(Pair(field.rawName, field.obfuscateName))
                replacePair.add(
                    Pair(
                        "this.${field.rawName}",
                        "this.${field.obfuscateName}"
                    )
                )
                replacePair.add(
                    Pair(
                        "${clazzInfo.obfuscateClazzName}.this.${field.rawName}",
                        "${clazzInfo.obfuscateClazzName}.this.${field.obfuscateName}"
                    )
                )
                if (field.isStatic() || field.isEnumElement) {
                    replacePair.add(
                        Pair(
                            "${clazzInfo.obfuscateClazzName}.${field.rawName}",
                            "${clazzInfo.obfuscateClazzName}.${field.obfuscateName}"
                        )
                    )
                }
            }
        }

        contentReplace(replacePair)
    }

    private fun replaceClassName() {
        val replacePair = mutableListOf<Pair<String, String>>()
        projectClazzInfos.forEach { clazzInfo ->

            //package name . class name
            replacePair.add(
                Pair(
                    clazzInfo.fullyQualifiedName,
                    clazzInfo.obfuscateClazzName
                )
            )

            if (clazzInfo.getClazzCanonicalName().contains(".")) {

                //is inner class or interface

                val raw = clazzInfo.getClazzCanonicalName().split(".")
                val obfuscate = clazzInfo.getObfuscateClazzCanonicalName().split(".")

                val size = raw.size

                //A.B.C
                repeat(size) {
                    val real = it + 1
                    replacePair.add(
                        Pair(
                            raw.takeLast(real).joinToString("."),
                            obfuscate.takeLast(real).joinToString(".")
                        )
                    )
                }

            } else {
                replacePair.add(
                    Pair(
                        clazzInfo.getClazzCanonicalName(),
                        clazzInfo.getObfuscateClazzCanonicalName()
                    )
                )
            }
        }

        contentReplace(replacePair)
    }

    private fun replaceImport() {
        val replacePair = mutableListOf<Pair<String, String>>()
        projectClazzInfos.forEach { clazzInfo: ClazzInfo ->
            replacePair.add(
                Pair(
                    clazzInfo.fullyQualifiedName,
                    clazzInfo.fullyObfuscateQualifiedName
                )
            )
            replacePair.add(
                Pair(
                    clazzInfo.getClazzCanonicalName(),
                    clazzInfo.getObfuscateClazzCanonicalName()
                )
            )
        }
        contentReplace(replacePair)
    }

    private fun replaceStaticField() {
        val replacePair = mutableListOf<Pair<String, String>>()
        projectClazzInfos.forEach { clazzInfo ->
            val needReplace = clazzInfo.fieldList.filter { it.isStatic() || it.isEnumElement }
            needReplace.forEach { fieldInfo ->
                replacePair.add(
                    Pair(
                        "${clazzInfo.fullyQualifiedName}.${fieldInfo.rawName}",
                        "${clazzInfo.fullyObfuscateQualifiedName}.${fieldInfo.obfuscateName}"
                    )
                )

                replacePair.add(
                    Pair(
                        "${clazzInfo.rawClazzName}.${fieldInfo.rawName}",
                        "${clazzInfo.obfuscateClazzName}.${fieldInfo.obfuscateName}"
                    )
                )

                replacePair.add(
                    Pair(
                        "${clazzInfo.getClazzCanonicalName()}.${fieldInfo.rawName}",
                        "${clazzInfo.getObfuscateClazzCanonicalName()}.${fieldInfo.obfuscateName}"
                    )
                )

                //replacePair.add(Pair(fieldInfo.rawName, fieldInfo.obfuscateName))

            }
        }

        contentReplace(replacePair)
    }

    private fun replaceStaticMethod() {
        val replacePair = mutableListOf<Pair<String, String>>()

        projectClazzInfos.forEach { clazzInfo ->
            val methodList = projectClazzInfos.flatMap { it.methodList }
            val staticMethod = methodList.filter { it.isStatic() }

            staticMethod.forEach { methodInfo ->
                replacePair.add(
                    Pair(
                        "${clazzInfo.fullyQualifiedName}.${methodInfo.rawName}",
                        "${clazzInfo.fullyObfuscateQualifiedName}.${methodInfo.obfuscateName}"
                    )
                )

                replacePair.add(
                    Pair(
                        "${clazzInfo.rawClazzName}.${methodInfo.rawName}",
                        "${clazzInfo.obfuscateClazzName}.${methodInfo.obfuscateName}"
                    )
                )

                replacePair.add(
                    Pair(
                        "${clazzInfo.getClazzCanonicalName()}.${methodInfo.rawName}",
                        "${clazzInfo.getObfuscateClazzCanonicalName()}.${methodInfo.obfuscateName}"
                    )
                )

                //replacePair.add(Pair(methodInfo.rawName, methodInfo.obfuscateName))
            }
        }

        contentReplace(replacePair)
    }

    private fun contentReplace(replaceMap: List<Pair<String, String>>) {
        val needReplace = replaceMap.toMutableList()
        needReplace.sortByDescending {
            it.first.length
        }
        needReplace.forEach {
            debug("replace ${it.first} to ${it.second}")
            fileContent = fileContent.replaceWords(it.first, it.second)
        }
    }


    private fun contentReplaceNoWord(replaceMap: List<Pair<String, String>>) {
        val needReplace = replaceMap.toMutableList()
        needReplace.sortByDescending {
            it.first.length
        }
        //use to .a or .a()
        //method and field
        needReplace.forEach {
            debug("replace ${it.first} to ${it.second}")
            fileContent = fileContent.replaceStartWithDot(it.first, it.second)
        }
    }

    override fun toString(): String = fileContent

    private fun debug(msg: String) {
        //println("model writer -> $msg")
    }
}
