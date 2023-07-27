package de.pluginwarden.data

import java.io.File

class StoragePlugin(val file: File) {

    val prefixes: List<String> by lazy {
        val versionFile = File(file, ".version")
        if (!versionFile.exists()) return@lazy emptyList<String>()
        versionFile.readLines()
    }

    val versions: List<Version> by lazy {
        file.listFiles { _, name -> name.endsWith(".md") }
            ?.map { it.nameWithoutExtension }
            ?.map(String::toVersion)
            ?.sortedDescending() ?: emptyList()
    }
}