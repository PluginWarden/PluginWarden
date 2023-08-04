@file:OptIn(ExperimentalCli::class)

package de.pluginwarden.commands

import com.github.ajalt.mordant.rendering.Lines
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.table.table
import de.pluginwarden.repository.getPluginStoragePlugin
import de.pluginwarden.repository.updatePluginStorage
import de.pluginwarden.t

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.widgets.HorizontalRule
import com.github.ajalt.mordant.widgets.Text
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
                tableBorders = Borders.ALL
                body {
                    possiblePlugin.versions.forEach {
                        cellBorders = Borders.LEFT_RIGHT
                        row {
                            cells(
                                it.storagePluginServerVersions.map { if (it.serverType == serverType) green(it.serverType.toString()) else it.serverType.toString() }.joinToString(", "),
                                getColor(it, installedPlugin)(it.version.toString())
                            )
                        }
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

            t.println("")
            t.println(table {
                captionTop(HorizontalRule(title = Text(name)))
                cellBorders = Borders.NONE
                body {
                    row {
                        cell(bold("Name")) {
                            align = TextAlign.RIGHT
                        }
                        cell(getColor(version, installedPlugin)(name))
                    }
                    row {
                        cell(bold("Version")) {
                            align = TextAlign.RIGHT
                        }
                        cell(version.version)
                    }
                    row {
                        cell(bold("Servers")) {
                            align = TextAlign.RIGHT
                        }
                        cell(version.storagePluginServerVersions.joinToString(", ") {
                            if (it.serverType == serverType) green(
                                it.serverType.toString()
                            ) else it.serverType.toString()
                        })
                    }
                    if (version.storagePluginIncompatibilities.isNotEmpty()) {
                        row {
                            cell(bold("Incompatibilities")) {
                                align = TextAlign.RIGHT
                            }
                            cell(version.storagePluginIncompatibilities.joinToString(", ") { red(it.pluginName) })
                        }
                    }

                    if(version.storagePluginDownloads.size == 1) {
                        row {
                            cell(bold("Download")) {
                                align = TextAlign.RIGHT
                            }
                            cell(if (downloadLink) version.storagePluginDownloads.first().link else URI.create(version.storagePluginDownloads.first().link.first).host)
                        }
                    }
                }
            })

            if (version.storagePluginDownloads.size != 1 && version.storagePluginDownloads.isNotEmpty()) {
                t.println()
                t.println(table {
                    captionTop("Download Links")
                    header {
                        row("Server", "Link")
                    }
                    body {
                        version.storagePluginDownloads.forEach {
                            row(it.serverType, if(downloadLink) it.link else URI.create(it.link.first).host)
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
                            row(underline(it.groupName), it.dependencies.map { "${it.first}:${it.second}" }.joinToString("\n"))
                        }
                    }
                })
            }
            t.println()
        }
    }
}