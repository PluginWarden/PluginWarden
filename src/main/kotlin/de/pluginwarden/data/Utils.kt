package de.pluginwarden.data

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles
import java.io.File

private val versionRegex = Regex("(?<MAJOR>\\d+)\\.(?<MINOR>\\d+)(\\.(?<PATCH>\\d+))?.*")

fun String?.toVersion(): Version {
    if (this == null) return Version(0, 0, 0)
    versionRegex.find(this)?.let {
        return Version(it.groups["MAJOR"]!!.value.toInt(), it.groups["MINOR"]!!.value.toInt(), it.groups["PATCH"]?.value?.toInt() ?: 0)
    }
    return Version(0, 0, 0)
}

val pwd = File(System.getProperty("user.dir"))

val serverType by lazy {
    ServerType.fromFile(pwd)?.first
}

val serverFile by lazy {
    ServerType.fromFile(pwd)?.second
}

val serverVersion by lazy {
    if (serverType == null) return@lazy null
    if (serverFile == null) return@lazy null
    serverType!!.getVersion(serverFile!!)
}

val pluginsDirectory by lazy {
    val pluginsFile = File(pwd, "plugins")
    if (!pluginsFile.exists() || !pluginsFile.isDirectory) return@lazy null
    return@lazy pluginsFile
}

val pluginsList by lazy {
    pluginsDirectory?.listFiles { _, name -> name.endsWith(".jar") }?.map(::InstalledPlugin)
}

fun getColor(plugin: StoragePluginVersion, installedPlugin: InstalledPlugin?): TextStyle {
    var style: TextStyle = TextStyles.reset + TextStyles.reset
    if (installedPlugin != null && plugin.version == installedPlugin.version) {
        style += TextStyles.bold
    }
    pluginsList?.forEach {pl ->
        if(plugin.storagePluginIncompatibilities.none { incompatibility -> incompatibility.pluginName == pl.name && !incompatibility.versionChecker(pl.version).first  }) {
            style += TextColors.green
        }
    }
    if(serverType != null && plugin.storagePluginServerVersions.any { sv -> sv.serverType == serverType && sv.compatibilityChecker(serverVersion!!).second }) {
        style += TextColors.yellow
    }
    if(serverType != null && plugin.storagePluginServerVersions.none { sv -> sv.serverType == serverType && sv.compatibilityChecker(serverVersion!!).first }) {
        style += TextColors.red
    }
    return style
}

fun StoragePluginVersion.isCompatible() = storagePluginServerVersions.any { sv ->
    sv.serverType == serverType && sv.compatibilityChecker(serverVersion!!).first } &&
        storagePluginIncompatibilities.none { ic ->
            pluginsList?.any { pl -> pl.file.nameWithoutExtension.startsWith(ic.pluginName, true) && !ic.versionChecker(pl.version).first } ?: false }
