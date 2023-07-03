package com.idguard

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

open class IdGuardTask @Inject constructor(
    private val variantName: String,
) : DefaultTask() {

    @TaskAction
    fun execute() {

    }
}