package de.pluginwarden

import java.io.File

fun main() {
    val pwd = File(System.getProperty("user.dir"))

    val serverType = ServerType.fromFile(pwd)
    if (serverType == null) {
        println("No server detected!")
        return
    }

    val version = serverType.first?.getVersion(serverType.second)
    if (version == null) {
        println("Unknown server version!")
        return
    }

    println("${serverType.first} -> ${serverType.first.getVersion(serverType.second)}")
}
