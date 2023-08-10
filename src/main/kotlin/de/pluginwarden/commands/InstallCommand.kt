@file:OptIn(ExperimentalCli::class)

package de.pluginwarden.commands

import com.github.ajalt.mordant.animation.progressAnimation
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.ConversionResult
import de.pluginwarden.commands.install.*
import de.pluginwarden.data.*
import de.pluginwarden.repository.updatePluginStorage
import de.pluginwarden.t
import de.pluginwarden.windows
import kotlinx.cli.*
import java.io.File
import java.io.FileInputStream
import java.net.URL
import java.util.zip.ZipInputStream

object InstallCommand : Subcommand("install", "Installs a plugin") {
    private val plugin by argument(ArgType.String, description = "The plugin to install").vararg()
    private val force by option(ArgType.Boolean, description = "Force install", shortName = "f").default(false)
    private val yes by option(ArgType.Boolean, description = "Answer yes to all questions", shortName = "y").default(false)
    private val dryRun by option(ArgType.Boolean, description = "Do not install the plugin, just show what would happen", shortName = "d").default(false)

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

    private fun prompt(question: String): Boolean {
        t.prompt(question, choices = listOf("Yes", "No"), default = "Yes") {
            if (it.startsWith("n")) ConversionResult.Valid("No")
            else if (it.startsWith("y") || it.startsWith("j")) ConversionResult.Valid("Yes")
            else ConversionResult.Invalid("Invalid choice")
        }.let {
            if (it == "No") return false
        }
        return true
    }

    fun download(link: Pair<String, String?>): File {
        val tmpFile = File.createTempFile("pluginwarden", if (link.second != null) ".zip" else ".jar")
        tmpFile.deleteOnExit()
        t.println("Downloading ${green(link.first)}...")
        URL(link.first).openStream().transferTo(tmpFile.outputStream())

        if (link.second != null) {
            ZipInputStream(FileInputStream(tmpFile)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == link.second) {
                        val tmpFile = File.createTempFile("pluginwarden", ".jar")
                        tmpFile.deleteOnExit()
                        tmpFile.outputStream().use { output ->
                            zip.copyTo(output)
                        }
                        return tmpFile
                    }
                    entry = zip.nextEntry
                }
            }
            throw IllegalStateException("Could not find ${link.second} in ${link.first}!")
        } else {
            return tmpFile
        }
    }


    private fun downloadAll(toInstall: List<StoragePluginVersion>): List<Pair<File, StoragePluginVersion>> {
        val progress = t.progressAnimation {
            text("Downloading plugins")
            percentage()
            if (windows) {
                progressBar(pendingChar = "─", completeChar = "─", separatorChar = "")
            } else {
                progressBar(separatorChar = "")
            }
            completed()
            timeRemaining()
        }
        progress.updateTotal(toInstall.size.toLong())
        progress.start()

        val toCopy = mutableListOf<Pair<File, StoragePluginVersion>>()
        toInstall.forEach { storagePluginVersion ->
            val downloadLink = storagePluginVersion.storagePluginDownloads.firstOrNull { dl ->
                dl.serverType?.isCompatibleWith(serverType!!)
                    ?: false
            }
                ?: storagePluginVersion.storagePluginDownloads.first()
            toCopy.add(download(downloadLink.link) to storagePluginVersion)
            progress.advance(1)
        }
        Thread.sleep(400)
        progress.stop()
        return toCopy
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
            }
                ?: notFound.add(it)
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
                if (i == null) ConversionResult.Invalid("Invalid choice")
                else if (i < 1 || i > current.second.second.size) ConversionResult.Invalid("Invalid choice")
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
                val installedPlugin = pluginsList?.firstOrNull { pl -> pl.storagePlugin == it.plugin }
                    ?: return@forEach
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
                    row(
                        it.first, it.second
                            ?: "???", red("Not found")
                    )
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

        if (warning || !yes) {
            t.println()
            if (!prompt("Do you want to install these plugins?")) {
                return
            }
        }

        t.println()
        val toInstall = downloadAll(notInstalled)

        if (dryRun) {
            t.println()
            t.println()
            t.println(red("Dry run, not installing plugins"))
            return
        }

        toInstall.forEach { (file, version) ->
            pluginsList?.forEach {
                if (it.storagePlugin == version.plugin) {
                    it.file.delete()
                }
            }
            file.copyTo(File(pluginsDirectory, "${version.plugin.prefixes.firstOrNull() ?: version.name.replace(" ", "")}-${version.version}.jar"), overwrite = true)
        }
    }
}