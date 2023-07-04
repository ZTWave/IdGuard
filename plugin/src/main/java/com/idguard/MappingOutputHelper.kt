package com.idguard

import org.gradle.api.Project
import java.io.BufferedWriter
import java.io.FileWriter

class MappingOutputHelper {
    companion object {
        fun write(project: Project, fileName: String, map: Map<String, String>) {
            val file = project.file(fileName)

            val writer = BufferedWriter(FileWriter(file, false))
            map.forEach { (s, s2) ->
                writer.write("$s -> $s2\n")
            }
            writer.flush()
            writer.close()
        }
    }
}