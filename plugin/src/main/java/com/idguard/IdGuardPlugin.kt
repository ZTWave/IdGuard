package com.idguard

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class IdGuardPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val android = project.extensions.getByName("android") as AppExtension
        project.afterEvaluate {
            println("variant is ${android.applicationVariants}")
            android.applicationVariants.all { variant ->
                val vName = variant.name.replaceFirstChar {
                    it.uppercaseChar()
                }
                it.tasks.create("LayoutGuard$vName", LayoutNameGuardTask::class.java, vName)
                it.tasks.create("IdGuard$vName", IdGuardTask::class.java, vName)
                it.tasks.create("ResGuard$vName", ResGuardTask::class.java, vName)
                it.tasks.create("ClassGuard$vName", ClassGuardTask::class.java, vName)
            }
        }
    }
}
