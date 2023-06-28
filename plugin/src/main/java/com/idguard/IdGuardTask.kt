package com.idguard

import com.android.build.gradle.BaseExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

open class IdGuardTask @Inject constructor(
    private val variantName: String,
) : DefaultTask() {

    @TaskAction
    fun execute() {
        val layoutDir = mutableListOf<File>()
        project.rootProject.subprojects {
            layoutDir.addAll(findLayoutDir(it, variantName))
        }
    }

    private fun findLayoutDir(project: Project, variantName: String): List<File> {
        val sourceSet = (extensions.getByName("android") as BaseExtension).sourceSets
        println("sourceSet")
        println(sourceSet)
        return emptyList()
    }
}