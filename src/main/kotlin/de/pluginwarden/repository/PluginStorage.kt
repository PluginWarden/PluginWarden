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

fun getPluginStoragePlugin(name: String): StoragePlugin? {
    val prefixDirectory = File(pluginStorageDirectory, name.substring(0..0))
    if (!prefixDirectory.exists()) return null
    val pluginDirectory = File(prefixDirectory, name)
    if (!pluginDirectory.exists()) return null
    return StoragePlugin(pluginDirectory)
}

private fun cmd(vararg command: String): String {
    return ProcessBuilder().directory(pluginStorageDirectory).command(*command).start().also {
        it.waitFor()
    }.inputStream.bufferedReader().use {
        it.readText()
    }
}