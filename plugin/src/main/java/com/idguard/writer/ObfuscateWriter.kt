package com.idguard.writer

import com.thoughtworks.qdox.model.JavaSource
import java.io.File

interface ObfuscateWriter {
    open fun writeSource(file: File, source: JavaSource)
}