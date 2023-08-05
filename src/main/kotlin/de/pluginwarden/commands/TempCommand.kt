package de.pluginwarden.commands

import com.github.ajalt.mordant.rendering.TextColors
import de.pluginwarden.commands.install.*
import de.pluginwarden.data.isCompatible
import de.pluginwarden.data.pluginsList
import de.pluginwarden.repository.getPluginStoragePlugin
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

    private fun toStoragePluginVersion(plugin: Pair<String, String?>, force: Boolean) {

    }

    override fun execute() {
        updatePluginStorage()
        if (pluginsList == null) {
            t.println(TextColors.red("Not inside a server directory!"))
            return
        }

        val plugins = toPlugins(plugin)

        plugins.forEach {
            val storagePlugin = getPluginStoragePlugin(it.first) ?: return@forEach
            if (it.second != null) {
                storagePlugin.versions.firstOrNull { version -> version.version.toString() == it.second }?.also {
                    add(it)
                }
            } else {
                storagePlugin.versions.firstOrNull { version -> version.isCompatible() }?.also {
                    add(it)
                }
            }
        }
        pluginsList?.forEach {
            it.storagePluginVersion?.let {
                add(it)
            }
        }

        while (resolve()) {
            // Do nothing
        }

        dependencyChoiceIncompatible()
        pluginIncompatible()

        println("DependenciesNotMet: ${dependenciesNotMet.joinToString(", ") { "${it.name}:${it.version}" }}")
        println("Incompatible: ${pluginIncompatibleWithOther.joinToString(", ") { "${it.name}:${it.version}" }}")
        println("Dependencies: ${dependencies.joinToString("; ") { "${it.first.name}:${it.first.version} -> ${it.second.second.joinToString(", ") { "${it.first}:${it.second}" }}" }}")
    }
}