@file:OptIn(ExperimentalCli::class)

package de.pluginwarden.commands

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.ConversionResult
import com.github.ajalt.mordant.widgets.HorizontalRule
import de.pluginwarden.data.*
import de.pluginwarden.repository.getPluginStoragePlugin
import de.pluginwarden.repository.updatePluginStorage
import de.pluginwarden.t
import kotlinx.cli.*
import java.io.File
import java.io.FileInputStream
import java.net.URL
import java.util.zip.ZipInputStream

object InstallCommand : Subcommand("install", "Installs a plugin") {
    private val plugin by argument(ArgType.String, description = "The plugin to install").vararg()
    private val force by option(ArgType.Boolean, description = "Force the installation of the plugin", shortName = "f").default(false)
    private val yes by option(ArgType.Boolean, description = "Answer yes to all questions", shortName = "y").default(false)

    fun download(link: Pair<String, String?>): File {
        val tmpFile = File.createTempFile("pluginwarden", if (link.second != null) ".zip" else ".jar")
        tmpFile.deleteOnExit()
        t.println("Downloading ${green(link.first)}...")
        URL(link.first).openStream().transferTo(tmpFile.outputStream())
        t.println("Downloaded ${green(link.first)}!")

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

    private fun getNotFindablePlugins(plugins: List<Pair<String, String?>>): List<Pair<String, String?>> {
        return plugins.filter { getPluginStoragePlugin(it.first) == null }
    }

    private fun getIncompatiblePlugins(plugins: List<Pair<String, String?>>): List<Pair<String, String?>> {
        return plugins.filter {
            val storagePluginVersion = it.convertToStoragePluginVersion()
                ?: return@filter true
            if (force && it.second != null) {
                return@filter false
            }

            fun checkDependencies(storagePluginVersion: StoragePluginVersion): Boolean {
                if (storagePluginVersion.storagePluginDependencies.isEmpty()) {
                    return false
                }

                return !storagePluginVersion.storagePluginDependencies.all {
                    return@all it.dependencies.any {
                        val plugin = (it.key to it.value.toString()).convertToStoragePluginVersion()
                            ?: return@any false
                        return@any checkDependencies(plugin)
                    }
                }
            }
            return@filter checkDependencies(storagePluginVersion)
        }
    }

    private fun Pair<String, String?>.convertToStoragePluginVersion(): StoragePluginVersion? {
        val storagePlugin = getPluginStoragePlugin(first) ?: return null
        return if (second != null) {
            storagePlugin.versions.firstOrNull { v -> v.version.toString() == second }
        } else {
            storagePlugin.versions.firstOrNull { v -> v.isCompatible() }
        }
    }

    private fun getPluginsToInstall(plugins: List<Pair<String, String?>>): List<StoragePluginVersion> {
        val toInstall: MutableList<StoragePluginVersion> = mutableListOf()
        val toIterate: MutableList<StoragePluginVersion> = mutableListOf()
        plugins.forEach { toIterate.add(it.convertToStoragePluginVersion()!!) }

        while (toIterate.isNotEmpty()) {
            val current = toIterate.removeAt(0)
            if (toInstall.contains(current)) continue
            toInstall.add(current)

            // TODO: Add dependencies
        }

        return toInstall
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

    override fun execute() {
        updatePluginStorage()
        if (pluginsList == null) {
            t.println(red("Not inside a server directory!"))
            return
        }

        val name = plugin.joinToString(" ")
        val plugins = name.split(",").map { it.trim() }
            .map {
                val split = it.split(":")
                if (split.size == 1) it to null
                else split[0] to split[1]
            }
            .toMutableList()

        getNotFindablePlugins(plugins).let {
            plugins.removeAll(it)
            if (it.isNotEmpty() && !yes) {
                t.println("Plugin${if (it.size == 1) "" else "s"} ${red(it.joinToString(", ", transform = { "${it.first}${if (it.second == null) "" else ":${it.second}"}" }))} not found!")
                if (plugins.isEmpty()) return
                if (!prompt("Should found plugin be installed?")) {
                    return
                }
            }
        }

        getIncompatiblePlugins(plugins).let {
            plugins.removeAll(it)
            if (it.isNotEmpty() && !yes) {
                t.println("Plugin${if (it.size == 1) "" else "s"} ${red(it.joinToString(", ", transform = { "${it.first}${if (it.second == null) "" else ":${it.second}"}" }))} ${if (it.size == 1) "is" else "are"} incompatible!")
                if (plugins.isEmpty()) return
                if (!prompt("Should every compatible plugin be installed?")) {
                    return
                }
            }
        }

        getPluginsToInstall(plugins);

        /*
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

        plToInstall.forEach {
            val installedPlugin = pluginsList?.find { plugin -> it.second.prefixes.any { plugin.file.nameWithoutExtension.startsWith(it, true) } }
            if (installedPlugin != null) {
                t.println("Plugin ${red(it.second.prefixes.first())} is already installed!")
                return
            }
            val file = it.first.storagePluginDownloads.firstOrNull { dl -> dl.serverType == serverType } ?: it.first.storagePluginDownloads.first()
            val filesToRemove = mutableListOf<File>()
            val tmp = download(file.link)

            val deps = it.first.storagePluginDependencies.map { d ->
                fun downloadDep(name: String, version: Version): File {
                    val p = getPluginStoragePlugin(name)
                    if (p == null) {
                        t.println("Plugin ${red(name)} not found!")
                        throw Exception("Plugin ${red(name)} not found!")
                    }
                    val v = p.versions.firstOrNull { it.version == version && (it.isCompatible() || force) }
                    if (v == null) {
                        t.println("Version ${red(version.toString())} of plugin ${red(p.prefixes.joinToString(" "))} not found!")
                        throw Exception("Version ${red(version.toString())} of plugin ${red(p.prefixes.joinToString(" "))} not found!")
                    }
                    val df = v.storagePluginDownloads.firstOrNull { dl -> dl.serverType == serverType } ?: v.storagePluginDownloads.first()
                    return download(df.link)
                }

                val installedDeps = pluginsList!!.filter { ip -> d.dependencies.any { dd -> dd.key == ip.name } }

                installedDeps.forEach { ip ->
                    if(d.dependencies[ip.name] == ip.version) {
                        t.println("Dependency ${red(ip.name)} is already installed!")
                        return@map null
                    } else {
                        if(!yes && !force) {
                            t.println("Dependency ${red(ip.name)} is already installed, but the wrong version!")
                            t.println("  Installed: ${ip.version}")
                            t.println("  Required: ${d.dependencies[ip.name]}")
                            t.prompt("Do you want to update the dependency?", choices = listOf("Yes", "No"), default = "Yes"){
                                if (it.startsWith("n")) ConversionResult.Valid("No")
                                else if (it.startsWith("y") || it.startsWith("j")) ConversionResult.Valid("Yes")
                                else ConversionResult.Invalid("Invalid choice")
                            }.let {
                                if (it == "No") return@map null
                                else filesToRemove.add(ip.file)
                            }
                        }
                    }
                }

                if(d.dependencies.size == 1) {
                    downloadDep(d.dependencies.entries.first().key, d.dependencies.entries.first().value) to d.dependencies.entries.first()
                } else {
                    t.println("Download Dependency ${red(d.groupName)}")
                    val validDeps = d.dependencies.entries.filter { vv -> getPluginStoragePlugin(vv.key)?.versions?.find { it.version == vv.value }?.isCompatible() ?: false };
                    validDeps.forEachIndexed { index, entry ->
                        t.println("  ${index + 1}. ${entry.key}:${entry.value}")
                    }

                    val dInstall = t.prompt("Which dependency do you want to download?", default = "1") {
                        val i = it.toIntOrNull()
                        if(i == null) ConversionResult.Invalid("Invalid choice")
                        else if(i < 1 || i > validDeps.size) ConversionResult.Invalid("Invalid choice")
                        else ConversionResult.Valid(validDeps.toList()[i - 1].key)
                    }

                    downloadDep(dInstall!!, d.dependencies[dInstall]!!) to d.dependencies.entries.first { it.key == dInstall }
                }
            }.filterNotNull()

            t.println("Installing ${green(it.second.prefixes.first())}...")

            filesToRemove.forEach { f ->
                t.println("Removing ${f.name}...")
                f.delete()
            }

            deps.forEach { (f, k) ->
                f.copyTo(File(pluginsDirectory, "${k.key}-${k.value}.jar"))
            }

            tmp.copyTo(File(pluginsDirectory, "${it.second.prefixes.first()}-${it.first.version}.jar"))

            t.println("Installed ${green(it.second.prefixes.first())}!")
        }
         */
    }
}