package de.pluginwarden.commands

import de.pluginwarden.repository.pluginStorageDirectory
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand

@OptIn(ExperimentalCli::class)
object RemoveCommand: Subcommand("remove", "Removes the PluginStorage") {

    override fun execute() {
        println("Removing PluginStorage...")
        pluginStorageDirectory.deleteRecursively()
    }
}
