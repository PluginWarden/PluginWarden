@file:OptIn(ExperimentalCli::class)

package de.pluginwarden.commands

import com.github.ajalt.mordant.table.table
import de.pluginwarden.data.pluginsList
import de.pluginwarden.repository.getPluginStoragePlugin
import de.pluginwarden.repository.updatePluginStorage
import de.pluginwarden.t
import kotlinx.cli.ArgType
import kotlinx.cli.Subcommand

import com.github.ajalt.mordant.rendering.TextColors.*
import kotlinx.cli.ExperimentalCli

object InfoCommand: Subcommand("info", "Shows information about a plugin") {
    private val plugin by argument(ArgType.String, description = "The plugin to show information about")

    override fun execute() {
        updatePluginStorage()

        val possiblePlugin = getPluginStoragePlugin(plugin)
        if (possiblePlugin == null) {
            println("Plugin not found!")
            return
        }

        val installedPlugin = pluginsList?.find { plugin -> possiblePlugin.prefixes.any { plugin.file.nameWithoutExtension.startsWith(it, true) } }

        t.println(table {
            header {
                row(plugin)
            }
            body {
                possiblePlugin.versions.forEach {
                    if (installedPlugin?.version == it) {
                        row(green(it.toString()))
                    } else {
                        row(it.toString())
                    }
                }
            }
        })
    }
}