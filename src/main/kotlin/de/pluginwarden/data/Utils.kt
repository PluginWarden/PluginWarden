package de.pluginwarden.data

import java.io.File
import kotlin.math.max

private val versionRegex = Regex("(?<MAJOR>\\d+)\\.(?<MINOR>\\d+)\\.(?<PATCH>\\d+)")

fun String?.toVersion(): Version {
    if (this == null) return Version(0, 0, 0)
    versionRegex.find(this)?.let {
        return Version(it.groups["MAJOR"]!!.value.toInt(), it.groups["MINOR"]!!.value.toInt(), it.groups["PATCH"]!!.value.toInt())
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

fun table(func: Table.() -> Unit) {
    Table().apply(func).print()
}

class Table {

    private val header = mutableListOf<String>()
    private val rows = mutableListOf<List<String>>()

    fun header(text: String): Table {
        if (rows.isNotEmpty()) throw IllegalStateException("Header must be defined before rows!")
        header.add(text)
        return this
    }

    fun row(vararg text: String): Table {
        if (header.isEmpty()) throw IllegalStateException("Rows must be defined after header!")
        if (text.size != header.size) throw IllegalArgumentException("Row must have same size as header!")
        rows.add(text.toList())
        return this
    }

    fun print() {
        val columnWidths = header.mapIndexed { index, headerString -> max(rows.map { it[index].length }.max()!!, headerString.length) }
        val rowFormat = "| ${columnWidths.joinToString(" | ") { "%-${it}s" }} |"
        val headerFormat = "|${columnWidths.joinToString("|") { "%-${it}s" }}|"

        println(rowFormat.format(*header.toTypedArray()))
        println(headerFormat.format(*columnWidths.map { "-".repeat(it + 2) }.toTypedArray()))
        rows.forEach {
            println(rowFormat.format(*it.toTypedArray()))
        }
    }
}