package de.pluginwarden.commands

import de.pluginwarden.data.*

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

        table {
            header(Text().append("Name"))
            header(Text().append("Version"))
            plugins.forEach {
                row(Text().append(it.file.nameWithoutExtension), Text().append(it.version.toString()))
            }
        }
    }
}