package com.idguard.utils

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.idguard.modal.ClazzInfo
import com.idguard.modal.FieldInfo
import com.idguard.modal.FunInfo
import java.io.File

fun File.parser(): List<ClazzInfo> {
    val infoList = mutableListOf<ClazzInfo>()
    val compilationUnit: CompilationUnit = JavaParser().parse(this).result.get()
    compilationUnit.findAll(ClassOrInterfaceDeclaration::class.java)
        .forEach { classOrInterfaceDeclaration ->
            val clazzname = classOrInterfaceDeclaration.name.toString()
            val isInnerClass = classOrInterfaceDeclaration.isInnerClass
            val isNestedClass = classOrInterfaceDeclaration.isNestedType

            val isInterface = classOrInterfaceDeclaration.isInterface

            println("inner class $clazzname isInnerClass -> $isInnerClass isNestedClass -> $isNestedClass isInterface -> $isInterface")
            println("")

            val rawPath = absolutePath
            //文件名称 对应的也是应该java中存在的class
            val rawClazzName = getRealName()
            val extensionName = getExtensionName()
            val obfuscateClazzName = RandomNameHelper.genClassName(Pair(4, 8))
            val obfuscatePath =
                parentFile.absolutePath + File.separator + obfuscateClazzName + extensionName
            val packageName = rawPath.getPackagePath()

            val methods = classOrInterfaceDeclaration.methods.map { declaration ->
                FunInfo(
                    modifier = declaration.modifiers.map { it.keyword.name },
                    name = declaration.nameAsString,
                    returnType = declaration.typeAsString,
                    params = declaration.parameters.map { "${it.type} ${it.name}" }
                )
            }

            val fields = classOrInterfaceDeclaration.fields.map { declaration ->
                val varb = declaration.variables.first.get()
                FieldInfo(
                    modifier = declaration.modifiers.map { it.keyword.name },
                    name = varb.nameAsString,
                    type = varb.type.asString()
                )
            }

            val info = ClazzInfo(
                packageName = packageName,
                rawPath = rawPath,
                rawClazzName = clazzname,
                obfuscateClazzName = obfuscateClazzName,
                obfuscatePath = obfuscatePath,
                methodList = methods,
                fieldList = fields,
                isInnerClass = isInnerClass,
                isNestedClass = isNestedClass,
                isInterface = isInterface,
            )
            infoList.add(info)
        }

    return infoList


    /*rawPath = file.absolutePath

    rawClazzName = file.getRealName()

    val extensionName = file.getExtensionName()

    obfuscateClazzName = RandomNameHelper.genClassName(Pair(4, 8))
    obfuscatePath =
        file.parentFile.absolutePath + File.separator + obfuscateClazzName + extensionName
    packageName = rawPath.getPackagePath()*/

}