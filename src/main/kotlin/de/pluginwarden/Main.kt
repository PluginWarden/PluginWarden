package de.pluginwarden

import java.io.File

fun main() {
    val pwd = File(System.getProperty("user.dir"))

    val serverType = ServerType.fromFile(pwd)
    println("${serverType?.first} -> ${serverType?.first?.getVersion(serverType.second)}")
}
