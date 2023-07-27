package de.pluginwarden.data

import org.json.JSONObject
import java.io.File
import java.util.jar.JarFile

class InstalledPlugin(val file: File) {

    val version: Version by lazy {
        val jarFile = JarFile(file)

        val pluginYML = jarFile.getJarEntry("plugin.yml")
        if (pluginYML != null) {
            val pluginYMLContent = jarFile.getInputStream(pluginYML).bufferedReader().readLines()
            val versionLine = pluginYMLContent.firstOrNull { it.startsWith("version:") }
            return@lazy versionLine?.split(":")?.get(1)?.trim().toVersion()
        }

        val bungeeYML = jarFile.getJarEntry("bungee.yml")
        if (bungeeYML != null) {
            val bungeeYMLContent = jarFile.getInputStream(bungeeYML).bufferedReader().readLines()
            val versionLine = bungeeYMLContent.firstOrNull { it.startsWith("version:") }
            return@lazy versionLine?.split(":")?.get(1)?.trim().toVersion()
        }

        val velocityJSON = jarFile.getJarEntry("velocity-plugin.json")
        if (velocityJSON != null) {
            val velocityJSONContent = jarFile.getInputStream(velocityJSON).bufferedReader().readLines().joinToString("")
            try {
                val versionLine = JSONObject(velocityJSONContent)["version"]
                return@lazy versionLine?.toString()?.trim().toVersion()
            } catch (e: Exception) {
                return@lazy Version(0, 0, 0)
            }
        }
        Version(0, 0, 0)
    }

    fun uninstall() {
        file.delete()
    }
}