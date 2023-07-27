@file:OptIn(ExperimentalCli::class)

package de.pluginwarden.commands

import com.github.ajalt.mordant.table.table
import de.pluginwarden.data.pluginsList
import de.pluginwarden.repository.getPluginStoragePlugin
import de.pluginwarden.repository.updatePluginStorage
import de.pluginwarden.t
import kotlinx.cli.ArgType
import kotlinx.cli.Subcommand
import kotlinx.cli.vararg

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles.*
import de.pluginwarden.data.serverType
import de.pluginwarden.data.serverVersion
import kotlinx.cli.ExperimentalCli

object InfoCommand: Subcommand("info", "Shows information about a plugin") {
    private val plugin by argument(ArgType.String, description = "The plugin to show information about").vararg()

    override fun execute() {
        updatePluginStorage()

        val name = plugin.joinToString(" ")
        val possiblePlugin = getPluginStoragePlugin(name)
        if (possiblePlugin == null) {
            println("Plugin not found!")
            return
        }

        val installedPlugin = pluginsList?.find { plugin -> possiblePlugin.prefixes.any { plugin.file.nameWithoutExtension.startsWith(it, true) } }

        t.println(table {
            header {
                row(name)
            }
            body {
                possiblePlugin.versions.forEach {
                    var style: TextStyle = reset + reset
                    if (installedPlugin != null && it.version == installedPlugin.version) {
                        style += bold
                    }
                    pluginsList?.forEach {pl ->
                        if(it.storagePluginIncompatibilities.none { incompatibility -> incompatibility.pluginName == pl.name && !incompatibility.versionChecker(pl.version).first  }) {
                            style += green
                        }
                    }
                    if(it.storagePluginServerVersions.any { sv -> sv.serverType == serverType && sv.compatibilityChecker(serverVersion!!).second }) {
                        style += yellow
                    }
                    if(it.storagePluginServerVersions.none { sv -> sv.serverType == serverType && sv.compatibilityChecker(serverVersion!!).first }) {
                        style += red
                    }
                    row(style(it.version.toString()))
                }
            }
        })
        /*
        t.println()
        t.println(table {
            header {
                row("Group", "Dependency")
            }
            body {
                possiblePlugin.versions[0].storagePluginDependencies.forEach {
                    row(
                        it.groupName,
                        it.dependencies.entries.map {
                            "${it.key}: ${it.value}"
                        }.joinToString("\n")
                    )
                }
            }
        })
         */
    }
}