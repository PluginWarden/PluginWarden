@file:OptIn(ExperimentalCli::class)

package de.pluginwarden.commands

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.table.Borders
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

    private fun Pair<String, String?>.convertToStoragePluginVersion(): Pair<StoragePluginVersion?, Availability> {
        val storagePlugin = getPluginStoragePlugin(first) ?: return null to Availability.NOT_FOUND
        return if (second != null) {
            val result = storagePlugin.versions.firstOrNull { v -> v.version.toString() == second }
            if (result == null) {
                null to Availability.NO_VALID_VERSION
            } else {
                result to Availability.FOUND
            }
        } else {
            val result = storagePlugin.versions.firstOrNull { v -> v.isCompatible() }
            if (result == null) {
                null to Availability.INCOMPATIBLE
            } else {
                result to Availability.FOUND
            }
        }
    }

    private fun StoragePluginVersion.isDeepCompatible(plugin: Pair<String, String?>): Compatibility {
        if (plugin.second != null && force) {
            println("Plugin ${plugin.first} ${this.shouldBeWarned()}")
            return if (this.shouldBeWarned()) Compatibility.WARNING else Compatibility.COMPATIBLE
        }
        if (!force && !this.isCompatible()) {
            return Compatibility.INCOMPATIBLE
        }

        val alreadyChecked = mutableMapOf<StoragePluginVersion, Boolean>()
        alreadyChecked[this] = true

        fun checkDependencies(storagePluginVersion: StoragePluginVersion): Compatibility {
            if (storagePluginVersion.storagePluginDependencies.isEmpty()) {
                return Compatibility.COMPATIBLE
            }

            val compatibilityList = storagePluginVersion.storagePluginDependencies.map {
                val compatibilityList = it.dependencies.map {
                    val pluginDep = (it.first to it.second.toString()).convertToStoragePluginVersion().also { (_, availability) ->
                        if (availability != Availability.FOUND) return@map Compatibility.INCOMPATIBLE
                    }.first!!
                    if (!pluginDep.isCompatible()) {
                        return@map Compatibility.INCOMPATIBLE
                    }
                    val isWarned = pluginDep.shouldBeWarned()
                    val result = checkDependencies(pluginDep)
                    if (result == Compatibility.INCOMPATIBLE) {
                        return@map Compatibility.INCOMPATIBLE
                    }
                    if (isWarned) {
                        return@map Compatibility.WARNING
                    }
                    return@map Compatibility.COMPATIBLE
                }

                if (compatibilityList.none { it == Compatibility.COMPATIBLE }) {
                    if (compatibilityList.none { it == Compatibility.WARNING }) {
                        return@map Compatibility.INCOMPATIBLE
                    }
                    return@map Compatibility.WARNING
                }
                return@map Compatibility.COMPATIBLE
            }

            if (compatibilityList.any { it == Compatibility.INCOMPATIBLE }) {
                return Compatibility.INCOMPATIBLE
            }
            if (compatibilityList.any { it == Compatibility.WARNING }) {
                return Compatibility.WARNING
            }
            return Compatibility.COMPATIBLE
        }
        val result = checkDependencies(this)
        return if (result == Compatibility.COMPATIBLE) {
            if (shouldBeWarned()) Compatibility.WARNING else Compatibility.COMPATIBLE
        } else {
            result
        }
    }

    private fun StoragePluginVersion.dependencyList(alreadySpecified: MutableList<Pair<String, String>>): MutableList<StoragePluginVersion> {
        val recurse = mutableListOf<StoragePluginVersion>()
        val toInstall = mutableListOf<StoragePluginVersion>()

        storagePluginDependencies.forEach {
            // Get all dependencies that are compatible or forced in the format Pair<StoragePluginVersion, String>
            val dependenciesToAdd = it.dependencies.map {
                // Convert dependency to StoragePluginVersion and check availability
                (it.first to it.second.toString()).convertToStoragePluginVersion().also { (spv, availability) ->
                    // If the dependency is not found and not forced, return null
                    if (!force && availability != Availability.FOUND) return@map null to it.first
                    // If the dependency is forced, return the dependency and the version
                    if (force) return@map spv to it.first
                }.first!! to it.first // Convert to Pair<StoragePluginVersion, String>
            }.filter { it.first != null }
                .map { it as Pair<StoragePluginVersion, String> }
                // Remove all dependencies that are incompatible or non if forced
                .filter { force || it.first.isCompatible() }

            // If any of the dependencies are already specified, there is no choise to make
            dependenciesToAdd.any {
                alreadySpecified.any { dep ->
                    dep.first == it.second && dep.second == it.first.version.toString()
                }
            }.yes {
                return@forEach
            }

            if (dependenciesToAdd.isEmpty()) {
                if (it.dependencies.isNotEmpty()) {
                    t.println("No compatible version of ${red(it.dependencies.first().first)} found!")
                    throw IllegalStateException("No compatible version of ${it.dependencies.first().first} found!")
                }
                return@forEach
            }
            if (dependenciesToAdd.size == 1) {
                dependenciesToAdd.first().let {
                    alreadySpecified.add(it.second to it.first.version.toString())
                    recurse.add(it.first)
                    toInstall.add(it.first)
                }
                return@forEach
            }

            // If there are multiple dependencies, ask the user which one to use
            val first = dependenciesToAdd.first()
            t.println("Download Dependency ${red(first.second)}")
            var default = 0
            dependenciesToAdd.forEachIndexed { index, entry ->
                if (force && default == 0 && entry.first.isCompatible()) {
                    default = index + 1
                }
                t.println("  ${index + 1}. ${entry.second}:${entry.first.version}")
            }
            if (default == 0) default = 1

            val dInstall = t.prompt("Which dependency do you want to download?", default = "$default") {
                val i = it.toIntOrNull()
                if(i == null) ConversionResult.Invalid("Invalid choice")
                else if(i < 1 || i > dependenciesToAdd.size) ConversionResult.Invalid("Invalid choice")
                else ConversionResult.Valid(dependenciesToAdd[i - 1])
            }
            if (dInstall is String) {
                if (dInstall as String == "$default") {
                    val result = dependenciesToAdd[default - 1]
                    alreadySpecified.add(result.second to result.first.version.toString())
                    recurse.add(result.first)
                    toInstall.add(result.first)
                }
                return@forEach
            }

            val result = dInstall as Pair<StoragePluginVersion, String>
            alreadySpecified.add(result.second to result.first.version.toString())
            recurse.add(result.first)
            toInstall.add(result.first)
        }

        recurse.forEach {
            toInstall.addAll(it.dependencyList(alreadySpecified))
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

    private inline fun Boolean.yes(action: () -> Unit) {
        if (this) action()
    }

    private inline fun Boolean.no(action: () -> Unit) {
        if (!this) action()
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

        val installable = mutableListOf<Pair<StoragePluginVersion, String>>()
        var hasWarning = false
        t.println(table {
            tableBorders = Borders.ALL
            captionTop("Plugins to install")
            header {
                row("Plugin", "Version", "Status")
            }
            body {
                cellBorders = Borders.LEFT_RIGHT
                plugins.forEach {
                    val storagePluginVersion = it.convertToStoragePluginVersion()
                    if (storagePluginVersion.second == Availability.NOT_FOUND) {
                        row(it.first, it.second ?: "???", red("Not found"))
                    } else if (storagePluginVersion.second == Availability.NO_VALID_VERSION) {
                        row(it.first, it.second ?: "???", red("No valid version"))
                    } else if (storagePluginVersion.second == Availability.INCOMPATIBLE) {
                        row(it.first, storagePluginVersion.first!!.version, yellow("Incompatible"))
                    } else {
                        val deepCompatability = storagePluginVersion.first!!.isDeepCompatible(it)
                        when(deepCompatability) {
                            Compatibility.COMPATIBLE -> {
                                row(it.first, storagePluginVersion.first!!.version, green("Compatible"))
                                installable.add(storagePluginVersion.first!! to it.first)
                            }
                            Compatibility.WARNING -> {
                                row(it.first, storagePluginVersion.first!!.version, yellow("Compatible"))
                                installable.add(storagePluginVersion.first!! to it.first)
                                hasWarning = true
                            }
                            Compatibility.INCOMPATIBLE -> {
                                row(it.first, storagePluginVersion.first!!.version, yellow("Incompatible"))
                            }
                        }
                    }
                }
            }
        })

        if (hasWarning) {
            t.println("Some plugins are not fully compatible with the server. They may not work as expected!")
        }
        if (hasWarning || !yes) {
            prompt("Do you want to install ${green("Compatible")} plugins?").no { return }
        }

        val alreadySpecified = mutableListOf<Pair<String, String>>()
        installable.forEach {
            alreadySpecified.add(it.second to it.first.version.toString())
        }
        val toInstall = mutableListOf<StoragePluginVersion>()
        toInstall.addAll(installable.map { it.first })
        installable.forEach {
            toInstall.addAll(it.first.dependencyList(alreadySpecified))
        }

        toInstall.forEach {
            t.println("Installing ${green(it.name)}:${green(it.version.toString())}")
        }
        println(toInstall.size)

        return

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