package com.idguard.modal

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.idguard.utils.RandomNameHelper
import com.idguard.utils.getExtensionName
import com.idguard.utils.getPackagePath
import com.idguard.utils.getRealName
import java.io.File

/**
 * 目前只针对java文件
 */
class ClazzInfo {

    constructor(file: File) {
        fillObInfo(file)
    }

    /**
     * 包名称
     */
    var packageName: String = ""
        private set

    /**
     * 原始路径
     */
    var rawPath: String = ""
        private set

    /**
     * 混淆后的路径
     */
    var obfuscatePath: String = ""
        private set

    /**
     * 原始类名称
     */
    var rawClazzName: String = ""
        private set

    /**
     * 混淆后的类名称
     */
    var obfuscateClazzName: String = ""
        private set

    var fileType: SourceFileType = SourceFileType.JAVA
        private set

    val filedList: List<FiledInfo> = mutableListOf()
        get() {
            return field.toList()
        }

    val funList: List<FunInfo> = mutableListOf()
        get() {
            return field.toList()
        }


    /**
     * 是否存在未混淆的引用
     */
    fun isThisClazzImported(file: File): Boolean {
        val content = file.readText()
        return content.contains("$packageName.$rawClazzName")
    }

    private fun fillObInfo(file: File) {
        rawPath = file.absolutePath
        rawClazzName = file.getRealName()
        val extensionName = file.getExtensionName()
        fileType = if (extensionName == ".java") {
            SourceFileType.JAVA
        } else {
            SourceFileType.KOTLIN
        }
        obfuscateClazzName = RandomNameHelper.genClassName(Pair(4, 8))
        obfuscatePath =
            file.parentFile.absolutePath + File.separator + obfuscateClazzName + extensionName
        packageName = rawPath.getPackagePath()

        val compilationUnit: CompilationUnit = JavaParser().parse(file).result.get()
        compilationUnit.findAll(MethodDeclaration::class.java)
            .forEach { methodDeclaration ->
                val modifiers = methodDeclaration.modifiers.map { it.keyword.name }
                val methodName = methodDeclaration.name
                val parameters =
                    methodDeclaration.parameters.joinToString { parameter -> parameter.typeAsString + " " + parameter.nameAsString }
                println("modifiers: $modifiers")
                println("Method: $methodName($parameters)")
            }

        compilationUnit.findAll(ClassOrInterfaceDeclaration::class.java)
            .forEach { classOrInterfaceDeclaration ->
                val clazzname = classOrInterfaceDeclaration.name
                val isInnerClass = classOrInterfaceDeclaration.isInnerClass
                val isNestedClass = classOrInterfaceDeclaration.isNestedType
                println("inner class $clazzname isInnerClass -> $isInnerClass isNestedClass -> $isNestedClass")
            }
    }

    override fun toString(): String {
        val sb = StringBuilder("")
        sb.append("rawPath -> $rawPath")
        sb.append("\n")
        sb.append("obfuscatePath -> $obfuscatePath")
        sb.append("\n")
        sb.append("rawClazzName -> $rawClazzName")
        sb.append("\n")
        sb.append("obfuscateClazzName -> $obfuscateClazzName")
        sb.append("\n")
        sb.append("packageName -> $packageName")
        return sb.toString()
    }
}