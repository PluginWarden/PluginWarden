package de.pluginwarden.commands

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.ConversionResult
import de.pluginwarden.commands.install.*
import de.pluginwarden.data.InstalledPlugin
import de.pluginwarden.data.StoragePluginVersion
import de.pluginwarden.data.Version
import de.pluginwarden.data.pluginsList
import de.pluginwarden.repository.updatePluginStorage
import de.pluginwarden.t
import kotlinx.cli.ArgType
import kotlinx.cli.Subcommand
import kotlinx.cli.default
import kotlinx.cli.vararg

object TempCommand : Subcommand("temp", "Temporary command") {
    private val plugin by argument(ArgType.String, description = "The plugin to install").vararg()
    private val force by option(ArgType.Boolean, description = "Force install", shortName = "f").default(false)

    private fun toPlugins(pluginList: List<String>): List<Pair<String, String?>> {
        return pluginList.joinToString(" ")
            .split(",")
            .map { it.trim() }
            .map {
                val split = it.split(":")
                if (split.size == 1) it to null
                else split[0] to split[1]
            }
    }

    override fun execute() {
        updatePluginStorage()
        if (pluginsList == null) {
            t.println(red("Not inside a server directory!"))
            return
        }

        val plugins = toPlugins(plugin)

        val notFound = mutableListOf<Pair<String, String?>>()
        plugins.forEach {
            it.toPluginVersion(force)?.also {
                add(it)
            } ?: notFound.add(it)
        }
        pluginsList?.forEach {
            it.storagePluginVersion?.let {
                add(it)
            }
        }

        while (true) {
            while (resolve()) {
                // Do nothing
            }

            while (dependencyChoiceIncompatible()) {
                // Do nothing
            }

            val current = dependencies.firstOrNull { it.second.second.size > 1 }
                ?: break

            t.println("Download Dependency for ${red(current.first.name)}, choice for ${red(current.second.first)}")
            current.second.second.forEachIndexed { index, entry ->
                t.println("  ${index + 1}. ${entry.first}:${entry.second}")
            }

            val dInstall = t.prompt("Which dependency do you want to download?", default = "1") {
                val i = it.toIntOrNull()
                if(i == null) ConversionResult.Invalid("Invalid choice")
                else if(i < 1 || i > current.second.second.size) ConversionResult.Invalid("Invalid choice")
                else ConversionResult.Valid(current.second.second[i - 1])
            }

            var result: StoragePluginVersion?
            if (dInstall is String) {
                if (dInstall as String == "1") {
                    result = current.second.second[0].toPluginVersion()!!
                } else {
                    continue
                }
            } else {
                result = (dInstall as Pair<String, Version>).toPluginVersion()!!
            }

            current.second.second.retainAll { it.first == result.name && it.second == result.version }
            add(result)
        }

        pluginIncompatible()

        val installedButVersionChangeNeeded = mutableMapOf<InstalledPlugin, Pair<Version, Version>>()
        val notInstalled = mutableListOf<StoragePluginVersion>()
        all.forEach {
            if (pluginsList?.any { pl -> pl.storagePlugin == it.plugin && pl.version == it.version } == false) {
                notInstalled.add(it)
            } else {
                val installedPlugin = pluginsList?.firstOrNull { pl -> pl.storagePlugin == it.plugin } ?: return@forEach
                if (installedPlugin.version != it.version) {
                    installedButVersionChangeNeeded[installedPlugin] = installedPlugin.version to it.version
                }
            }
        }

        var warning = false
        t.println()
        t.println(table {
            captionTop("Plugins to install")
            tableBorders = Borders.ALL
            header {
                row("Plugin", "Version", "Status")
            }
            body {
                cellBorders = Borders.LEFT_RIGHT

                notFound.forEach {
                    row(it.first, it.second ?: "???", red("Not found"))
                }
                dependenciesNotMet.forEach {
                    row(it.name, it.version, red("Dependencies not met"))
                }
                pluginIncompatibleWithOther.forEach {
                    row(it.name, it.version, red("Incompatible with other plugins"))
                }
                installedButVersionChangeNeeded.forEach {
                    row(it.key.storagePlugin?.name, "${it.value.first} -> ${it.value.second}", yellow("Version change needed"))
                }
                notInstalled.forEach {
                    if (it.isWarning()) {
                        row(it.name, it.version, yellow("Installable"))
                        warning = true
                    } else {
                        row(it.name, it.version, green("Installable"))
                    }
                }
            }
        })

        if (warning) {
            t.println()
            t.println(yellow("Some plugins are marked as installable, but may not work correctly or are fully supported!"))
            t.println(yellow("Please check the plugin page for more information!"))
        }
    }
}