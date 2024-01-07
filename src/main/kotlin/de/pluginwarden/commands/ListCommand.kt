package de.pluginwarden.commands

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import de.pluginwarden.data.*
import de.pluginwarden.repository.updatePluginStorage
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

        updatePluginStorage()
        val plugins = pluginsList.let {
            if (it!!.isEmpty()) {
                println("No plugins installed!")
                return
            }
            return@let it
        }

        t.println(table {
            tableBorders = Borders.ALL
            header {
                row("Plugin", "Version", "Updatable")
            }
            body {
                cellBorders = Borders.LEFT_RIGHT
                plugins.forEach {
                    val storagePlugin = it.storagePlugin
                    if (storagePlugin == null) {
                        row(it.file.nameWithoutExtension, it.version.toString(), yellow("Unknown"))
                        return@forEach
                    }
                    val compatible = storagePlugin.versions.takeWhile { v -> v.version > it.version }.firstOrNull { it.isCompatible() }
                    row(it.file.nameWithoutExtension, it.version.toString(), if (compatible != null) green("Yes") + " -> " + compatible.version.toString() else red("No"))
                }
            }
        })
    }
}