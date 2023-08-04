@file:OptIn(ExperimentalCli::class)

package de.pluginwarden.commands

import de.pluginwarden.repository.searchStoragePlugin
import de.pluginwarden.repository.updatePluginStorage
import de.pluginwarden.t

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import de.pluginwarden.data.getColor
import kotlinx.cli.*

object SearchCommand : Subcommand("search", "Searches for plugins") {
    private val query by argument(ArgType.String, description = "The query to search for").vararg()
    private val and by option(ArgType.Boolean, description = "Spaces as And", shortName = "a").default(false)

    override fun execute() {
        updatePluginStorage()

        val query = query.joinToString(" ")
        val plugins = searchStoragePlugin(query, and)

        if (plugins.isEmpty()) {
            t.println(red("No plugins found!"))
            return
        }

        t.println()
        t.println(table {
            captionTop("${plugins.size} plugin${if (plugins.size != 1) "s" else ""} found")
            header {
                row("Name", "Version", "Server")
            }
            tableBorders = Borders.ALL
            body {
                plugins.forEach {
                    row {
                        cellBorders = Borders.LEFT_RIGHT
                        cells(it.prefixes.joinToString(" "), getColor(it.versions.first(), null)(it.versions.first().version.toString()), it.versions.first().storagePluginServerVersions.joinToString(", ") { sv -> sv.serverType.toString() })
                    }
                }
            }
        })
    }
}