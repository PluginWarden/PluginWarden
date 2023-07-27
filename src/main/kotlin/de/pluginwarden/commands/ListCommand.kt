package de.pluginwarden.commands

import de.pluginwarden.data.*
import kotlin.math.max

object ListCommand: Command {

    override fun execute(args: List<String>) {
        if (serverType == null) {
            println("No server detected!")
            return
        }
        if (serverVersion == null) {
            println("Unknown server version!")
            return
        }
        if (pluginsDirectory == null) {
            println("No plugins directory found!")
            return
        }

        val plugins = pluginsList.let {
            if (it!!.isEmpty()) {
                println("No plugins installed!")
                return
            }
            return@let it
        }

        val nameLength = max(plugins.map { it.file.nameWithoutExtension }.maxBy { it.length }.length, 4)
        val versionLength = max(plugins.map { it.version.toString() }.maxBy { it.length }.length, 7)

        table {
            header("Name")
            header("Version")
            plugins.forEach {
                row(it.file.nameWithoutExtension, it.version.toString())
            }
        }
    }
}