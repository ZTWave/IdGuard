package com.idguard

import com.android.build.gradle.AppExtension
import com.idguard.extension.IdGuardExtension
import com.idguard.tasks.ClassGuardTask
import com.idguard.tasks.IdGuardTask
import com.idguard.tasks.LayoutNameGuardTask
import com.idguard.tasks.ProguardDicGenTask
import com.idguard.tasks.ResGuardTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class IdGuardPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val android = project.extensions.getByName("android") as AppExtension

        val idGuardExtension = project.extensions.create("idGuard", IdGuardExtension::class.java)

        project.afterEvaluate {
            println("variant is ${android.applicationVariants}")
            android.applicationVariants.all { variant ->
                val vName = variant.name.replaceFirstChar {
                    it.uppercaseChar()
                }
                it.tasks.create("LayoutGuard$vName", LayoutNameGuardTask::class.java, vName)
                it.tasks.create("IdGuard$vName", IdGuardTask::class.java, vName)
                it.tasks.create("ResGuard$vName", ResGuardTask::class.java, vName)

                it.tasks.register(
                    "ClassGuard$vName",
                    ClassGuardTask::class.java,
                    vName,
                    idGuardExtension.whiteList
                )
            }
            it.tasks.create(
                "ProguardDicGen",
                ProguardDicGenTask::class.java,
                idGuardExtension.dictCapacity
            )
        }
    }
}
