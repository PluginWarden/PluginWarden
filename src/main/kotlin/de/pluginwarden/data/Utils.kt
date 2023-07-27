package de.pluginwarden.data

import java.io.File
import kotlin.math.max

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
    pluginsDirectory?.listFiles { dir, name -> name.endsWith(".jar") }?.map(::InstalledPlugin)
}
