package de.pluginwarden.commands

import com.github.ajalt.mordant.table.table
import de.pluginwarden.data.*
import de.pluginwarden.t
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand

@OptIn(ExperimentalCli::class)
object ListCommand: Subcommand("list", "Lists all installed plugins") {

    override fun execute() {
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

        t.println(table {
            header {
                row("Plugin", "Version")
            }
            body {
                plugins.forEach {
                    row(it.file.nameWithoutExtension, it.version.toString())
                }
            }
        })
    }
}