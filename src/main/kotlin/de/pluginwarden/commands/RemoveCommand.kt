package de.pluginwarden.commands

import de.pluginwarden.repository.pluginStorageDirectory

object RemoveCommand: Command {

    override fun execute(args: List<String>) {
        println("Removing PluginStorage...")
        pluginStorageDirectory.deleteRecursively()
    }
}
