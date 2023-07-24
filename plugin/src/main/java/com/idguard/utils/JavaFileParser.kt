package com.idguard.utils

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.idguard.modal.ClazzInfo
import com.idguard.modal.FieldInfo
import com.idguard.modal.MethodInfo
import java.io.File

fun File.parser(): List<ClazzInfo> {
    val infoList = mutableListOf<ClazzInfo>()
    val compilationUnit: CompilationUnit = JavaParser().parse(this).result.get()

    val rawFileName = getRealName()

    compilationUnit.findAll(ClassOrInterfaceDeclaration::class.java)
        .forEach { classOrInterfaceDeclaration ->
            val clazzname = classOrInterfaceDeclaration.name.toString()
            val isInnerClass = classOrInterfaceDeclaration.isInnerClass
            val isNestedClass = classOrInterfaceDeclaration.isNestedType
            val isInterface = classOrInterfaceDeclaration.isInterface

            val rawPath = absolutePath
            val obfuscateClazzName = RandomNameHelper.genClassName(Pair(4, 8))

            /**
             * attention random is not safety, in some cases may generate same names!!.
             */
            val methods = classOrInterfaceDeclaration.methods.map { declaration ->
                val isOverride = declaration.annotations.find {
                    it.nameAsString.contains(
                        "override",
                        true
                    )
                } != null
                val obfuscateName = if (isOverride) {
                    ""
                } else {
                    RandomNameHelper.genNames(1, Pair(4, 12), false, true).first()
                }
                MethodInfo(
                    modifier = declaration.modifiers.map { it.keyword.name },
                    name = declaration.nameAsString,
                    returnType = declaration.typeAsString,
                    params = declaration.parameters.map { "${it.type} ${it.name}" },
                    isOverride = isOverride,
                    obfuscateName = obfuscateName
                )
            }

            val fields = classOrInterfaceDeclaration.fields.map { declaration ->
                val varb = declaration.variables.first.get()
                val obfuscateName = RandomNameHelper.genNames(1, Pair(2, 8), false, true).first()
                FieldInfo(
                    modifier = declaration.modifiers.map { it.keyword.name },
                    name = varb.nameAsString,
                    type = varb.type.asString(),
                    obfuscateName = obfuscateName
                )
            }

            var packageName = ""

            val imports = mutableListOf<String>()
            val fileLines = this.readLines()
            fileLines.forEach {
                if (it.startsWith("import ")) {
                    imports.add(it.removePrefix("import ").removeSuffix(";"))
                }
                if (it.startsWith("package ")) {
                    packageName = it.removePrefix("package ").removeSuffix(";")
                }
            }

            val implNames = classOrInterfaceDeclaration.implementedTypes.map { it.nameAsString }

            val extendName =
                classOrInterfaceDeclaration.extendedTypes.map { it.nameAsString }.firstOrNull()
                    ?: ""

            val info = ClazzInfo(
                packageName = packageName,
                rawPath = rawPath,
                rawClazzName = clazzname,
                obfuscateClazzName = obfuscateClazzName,
                methodList = methods,
                fieldList = fields,
                isInnerClass = isInnerClass,
                isNestedClass = isNestedClass,
                isInterface = isInterface,
                belongFile = this,
                imports = imports,
                implName = implNames,
                extendName = extendName,
                bodyInfo = classOrInterfaceDeclaration.tokenRange.get().toString()
            )
            infoList.add(info)
        }

    infoList.forEach { info ->

        if (!info.isNestedClass && !info.isInnerClass) {
            val obfuscatePath =
                parentFile.absolutePath + File.separator + info.obfuscateClazzName + info.belongFile.getExtensionName()
            info.obfuscatePath = obfuscatePath
            return@forEach
        }
        //不用担心引用一个java文件中不在java文件名相同类名外部的那个类 编写过程中会报错
        val matchNodes =
            infoList.filter { it.bodyInfo.contains(info.bodyInfo) }.sortedBy { it.bodyInfo.length }
        //如果是嵌入的 从小到大 为 自身 -> 一级嵌套 
        val parentInfoNode = matchNodes.getOrNull(1)
        parentInfoNode?.let {
            info.parentNode = it
            info.obfuscatePath = it.obfuscatePath
        }
    }

    return infoList

}