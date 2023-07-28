package de.pluginwarden.commands

import de.pluginwarden.repository.updatePluginStorage
import kotlinx.cli.ArgType
import kotlinx.cli.Subcommand
import kotlinx.cli.vararg

object UninstallCommand: Subcommand("uninstall", "Uninstall a plugin") {
    private val plugin by argument(ArgType.String, description = "The plugin to install").vararg()

    override fun execute() {
        updatePluginStorage()
        val name = plugin.joinToString(" ")
        val plugins = name.split(",").map { it.trim() }

        println("Uninstalling ${plugins.joinToString(", ")}...")
    }
}