package com.idguard.utils

import org.gradle.api.Project
import java.io.BufferedWriter
import java.io.FileWriter

class MappingOutputHelper {
    companion object {
        fun write(
            project: Project,
            fileName: String,
            map: Map<String, String>,
            append: Boolean = false
        ) {
            val file = project.file(fileName)

            val writer = BufferedWriter(FileWriter(file, append))
            map.forEach { (s, s2) ->
                writer.write("$s -> $s2\n")
            }
            writer.flush()
            writer.close()
        }

        fun appendNewLan(project: Project, fileName: String, lineText: String) {
            val file = project.file(fileName)
            val writer = BufferedWriter(FileWriter(file, true))
            writer.write(lineText + "\n")
            writer.flush()
            writer.close()
        }

        fun clearText(project: Project, fileName: String){
            val file = project.file(fileName)
            file.writeText("")
        }
    }
}