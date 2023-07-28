package de.pluginwarden.commands

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.ConversionResult
import com.github.ajalt.mordant.widgets.HorizontalRule
import de.pluginwarden.data.*
import de.pluginwarden.repository.getPluginStoragePlugin
import de.pluginwarden.repository.updatePluginStorage
import de.pluginwarden.t
import kotlinx.cli.ArgType
import kotlinx.cli.Subcommand
import kotlinx.cli.default
import kotlinx.cli.vararg

object InstallCommand: Subcommand("install", "Installs a plugin") {
    private val plugin by argument(ArgType.String, description = "The plugin to install").vararg()
    private val force by option(ArgType.Boolean, description = "Force the installation of the plugin", shortName = "f").default(false)
    private val yes by option(ArgType.Boolean, description = "Answer yes to all questions", shortName = "y").default(false)

    override fun execute() {
        updatePluginStorage()
        val name = plugin.joinToString(" ")
        val plugins = name.split(",")

        val plToInstall = plugins.map { pl ->
            val vSplit = pl.split(":")
            val p = getPluginStoragePlugin(vSplit[0])
            if (p == null) {
                t.println("Plugin ${red(pl)} not found!")
                return
            }
            if (vSplit.size == 1) {
                val pp = p.versions.firstOrNull { it.isCompatible() }
                if (pp == null) {
                    t.println("No compatible version of plugin ${red(p.prefixes.joinToString(" "))} found!")
                    return
                }
                pp to p
            } else {
                val v = p.versions.firstOrNull { it.version.toString() == vSplit[1] && (it.isCompatible() || force)}
                if (v == null) {
                    t.println("Version ${red(vSplit[1])} of plugin ${red(p.prefixes.joinToString(" "))} not found!")
                    return
                }
                v to p
            }
        }

        t.println(table {
            captionTop(HorizontalRule("Plugins to install"))
            header {
                row("Plugin", "Version")
            }
            body {
                plToInstall.forEach {
                    row(it.second.prefixes.first(), it.first.version.toString())
                }
            }
        })

        t.println()

        if (!yes) {
            t.prompt("Do you want to install these plugins?", choices = listOf("Yes", "No"), default = "Yes"){
                if (it.startsWith("n")) ConversionResult.Valid("No")
                else if (it.startsWith("y") || it.startsWith("j")) ConversionResult.Valid("Yes")
                else ConversionResult.Invalid("Invalid choice")
            }.let {
                if (it == "No") return
            }
        }
    }
}