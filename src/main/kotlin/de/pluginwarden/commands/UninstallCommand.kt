package de.pluginwarden.commands

import com.github.ajalt.mordant.rendering.TextColors.red
import de.pluginwarden.data.pluginsList
import de.pluginwarden.repository.updatePluginStorage
import de.pluginwarden.t
import kotlinx.cli.ArgType
import kotlinx.cli.Subcommand
import kotlinx.cli.vararg

object UninstallCommand: Subcommand("uninstall", "Uninstall a plugin") {
    private val plugin by argument(ArgType.String, description = "The plugin to install").vararg()

    override fun execute() {
        if (pluginsList == null) {
            t.println(red("Not inside a server directory!"))
            return
        }

        updatePluginStorage()
        val name = plugin.joinToString(" ")
        val plugins = name.split(",").map { it.trim() }


    }
}