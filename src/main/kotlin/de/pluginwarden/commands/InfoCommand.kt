package de.pluginwarden.commands

import de.pluginwarden.data.Text
import de.pluginwarden.data.pluginsList
import de.pluginwarden.data.table
import de.pluginwarden.repository.getPluginStoragePlugin
import de.pluginwarden.repository.updatePluginStorage
import org.fusesource.jansi.Ansi.Color

object InfoCommand: Command {

    override fun execute(args: List<String>) {
        if (args.isEmpty()) {
            println("No plugin specified!")
            return
        }

        updatePluginStorage()

        val name = args.joinToString(" ")
        val possiblePlugin = getPluginStoragePlugin(name)
        if (possiblePlugin == null) {
            println("Plugin not found!")
            return
        }

        val installedPlugin = pluginsList?.find { plugin -> possiblePlugin.prefixes.any { plugin.file.nameWithoutExtension.startsWith(it, true) } }

        table {
            header(Text().append(name))
            possiblePlugin.versions.forEach {
                if (installedPlugin?.version == it) {
                    row(Text().fgBright(Color.GREEN).append(it.toString()).reset())
                } else {
                    row(Text().append(it.toString()))
                }
            }
        }
    }
}