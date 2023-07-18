package com.idguard.utils

import com.android.build.gradle.BaseExtension
import org.gradle.api.Project
import java.io.File

fun Project.findLayoutDirs(variantName: String) = findDirsInRes(variantName, "layout")

fun Project.findLayoutUsagesInRes(variantName: String) =
    findDirsInRes(variantName, "layout", "values")

fun Project.findDirsInRes(variantName: String, vararg dirName: String): ArrayList<File> {
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

fun Project.javaDirs(variantName: String): List<File> {
    val sourceSet = (extensions.getByName("android") as BaseExtension).sourceSets
    val nameSet = mutableSetOf<String>()
    nameSet.add("main")
    if (isAndroidProject()) {
        nameSet.addAll(variantName.splitWords())
    }
    val javaDirs = mutableListOf<File>()
    sourceSet.names.forEach { name ->
        if (nameSet.contains(name)) {
            sourceSet.getByName(name).java.srcDirs.mapNotNullTo(javaDirs) {
                if (it.exists()) it else null
            }
        }
    }
    return javaDirs
}

fun Project.isAndroidProject() =
    plugins.hasPlugin("com.android.application")
        || plugins.hasPlugin("com.android.library")

//返回manifest文件目录,有且仅有一个
fun Project.manifestFile(): File {
    val sourceSet = (extensions.getByName("android") as BaseExtension).sourceSets
    return sourceSet.getByName("main").manifest.srcFile
}

fun Project.findPackageName(): String {
    val namespace = findNameSpaceInGradle()
    val packageName = findPackageInManifest()
    return if (namespace == "") {
        packageName
    } else {
        namespace
    }
}

fun Project.findPackageInManifest(): String {
    val manifestPackageRegex = Regex("package=\".+\"")
    val packageStr =
        manifestPackageRegex.find(manifestFile().readText())?.groupValues?.first() ?: return ""
    return packageStr.removePrefix("package=").removeSurrounding("\"")
}

fun Project.findNameSpaceInGradle(): String {
    val buildGradleRegex = Regex("namespace .+")
    val namespaceStr =
        buildGradleRegex.find(buildFile.readText())?.groupValues?.first() ?: return ""
    val namespace = namespaceStr.removePrefix("namespace").trim()
    return namespace.removeSurrounding("\'").removeSurrounding("\"")
}