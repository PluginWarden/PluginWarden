package de.pluginwarden.repository

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

private fun cmd(vararg command: String): String {
    return ProcessBuilder().directory(pluginStorageDirectory).command(*command).start().also {
        it.waitFor()
    }.inputStream.bufferedReader().use {
        it.readText()
    }
}