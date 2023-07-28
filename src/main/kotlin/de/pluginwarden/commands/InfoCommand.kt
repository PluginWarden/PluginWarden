@file:OptIn(ExperimentalCli::class)

package de.pluginwarden.commands

import com.github.ajalt.mordant.table.table
import de.pluginwarden.repository.getPluginStoragePlugin
import de.pluginwarden.repository.updatePluginStorage
import de.pluginwarden.t

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import de.pluginwarden.data.*
import kotlinx.cli.*
import java.net.URI

object InfoCommand: Subcommand("info", "Shows information about a plugin") {
    private val plugin by argument(ArgType.String, description = "The plugin to show information about").vararg()
    private val version by option(ArgType.String, description = "The version of the plugin to show information about", shortName = "v")
    private val downloadLink by option(ArgType.Boolean, description = "Show the full Download link", shortName = "d").default(false)

    override fun execute() {
        updatePluginStorage()

        val name = plugin.joinToString(" ")
        val possiblePlugin = getPluginStoragePlugin(name)
        if (possiblePlugin == null) {
            println("Plugin not found!")
            return
        }

        val installedPlugin = pluginsList?.find { plugin -> possiblePlugin.prefixes.any { plugin.file.nameWithoutExtension.startsWith(it, true) } }

        if(version == null) {
            t.println(table {
                captionTop(name)
                header {
                    row("Servers", "Version")
                }
                body {
                    possiblePlugin.versions.forEach {
                        row(
                            it.storagePluginServerVersions.map { if (it.serverType == serverType) green(it.serverType.toString()) else it.serverType.toString() }.joinToString(", "),
                            getColor(it, installedPlugin)(it.version.toString())
                        )
                    }
                }
            })
        } else {
            val version = possiblePlugin.versions.firstOrNull { it.version.toString() == version }
            if(version == null) {
                println(red(bold("Version not found!")))
                println(bold("Available Versions:"))
                println("  ${possiblePlugin.versions.joinToString(", ") { it.version.toString() }}")
                return
            }

            t.println("${underline("Name")}: ${getColor(version, installedPlugin)(name)}")
            t.println("${underline("Version")}: ${version.version}")
            t.println("${underline("Servers")}: ${version.storagePluginServerVersions.map { if (it.serverType == serverType) green(it.serverType.toString()) else it.serverType.toString() }.joinToString(", ")}")
            if(version.storagePluginIncompatibilities.isNotEmpty()) {
                t.println("${underline("Incompatibilities")}: ${version.storagePluginIncompatibilities.joinToString(", ") { red(it.pluginName) }}")
            }
            if(version.storagePluginDownloads.size == 1) {
                t.println("${underline("Download")}: ${if(downloadLink) version.storagePluginDownloads.first().link else URI.create(version.storagePluginDownloads.first().link).host}")
            } else {
                t.println()
                t.println(table {
                    captionTop("Download Links")
                    header {
                        row("Server", "Link")
                    }
                    body {
                        version.storagePluginDownloads.forEach {
                            row(it.serverType, if(downloadLink) it.link else URI.create(it.link).host)
                        }
                    }
                })
            }
            if(version.storagePluginDependencies.isNotEmpty()) {
                t.println()
                t.println(table {
                    captionTop("Dependencies")
                    header {
                        row("Group", "Options")
                    }
                    body {
                        version.storagePluginDependencies.forEach {
                            row(underline(it.groupName), it.dependencies.map { "${it.key}:${it.value}" }.joinToString("\n"))
                        }
                    }
                })
            }
        }
    }
}