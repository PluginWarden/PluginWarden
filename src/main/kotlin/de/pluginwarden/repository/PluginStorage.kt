package de.pluginwarden.repository

import de.pluginwarden.data.StoragePlugin
import java.io.File

val pluginStorageDirectory = File(System.getProperty("user.home"), ".pluginwarden")

fun updatePluginStorage() {
    if (!pluginStorageDirectory.exists()) {
        println("Initializing PluginStorage...")
        pluginStorageDirectory.mkdirs()
        cmd("git", "clone", "https://github.com/PluginWarden/PluginStorage.git", pluginStorageDirectory.absolutePath)
    } else {
        cmd("git", "pull")
    }
}

val allPlugins by lazy {
    pluginStorageDirectory.listFiles()?.filter { it.isDirectory }?.map { it.listFiles() }?.flatMap {
        it?.map { pl -> StoragePlugin(pl) }
            ?: listOf()
    }
}

fun getPluginStoragePlugin(name: String): StoragePlugin? {
    val prefixDirectory = File(pluginStorageDirectory, name.substring(0..0))
    if (!prefixDirectory.exists()) return null
    val pluginDirectory = File(prefixDirectory, name)
    if (!pluginDirectory.exists()) return null
    return StoragePlugin(pluginDirectory)
}

fun searchStoragePlugin(query: String, spaceAnd: Boolean): List<StoragePlugin> {
    val plugins = mapOf(
        *allPlugins!!.map {
            "${it.prefixes.joinToString(" ")} ${
                it.prefixes.joinToString(" ") { n ->
                    n.toCharArray().filter { c -> c.isUpperCase() }.joinToString("")
                }
            }" to it
        }.toTypedArray()
    )

    return plugins.filter { if (spaceAnd) query.split(" ").all { q -> it.key.contains(q, true) } else query.split(" ").any { q -> it.key.contains(q, true) } }.map { it.value }
}

private fun cmd(vararg command: String): String {
    return ProcessBuilder().directory(pluginStorageDirectory).command(*command).start().also {
        it.waitFor()
    }.inputStream.bufferedReader().use {
        it.readText()
    }
}