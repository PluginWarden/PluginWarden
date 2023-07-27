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

    val name: String by lazy {
        val jarFile = JarFile(file)

        val pluginYML = jarFile.getJarEntry("plugin.yml")
        if (pluginYML != null) {
            val pluginYMLContent = jarFile.getInputStream(pluginYML).bufferedReader().readLines()
            val nameLine = pluginYMLContent.firstOrNull { it.startsWith("name:") }
            return@lazy nameLine?.split(":")?.get(1)?.trim() ?: "Unknown"
        }

        val bungeeYML = jarFile.getJarEntry("bungee.yml")
        if (bungeeYML != null) {
            val bungeeYMLContent = jarFile.getInputStream(bungeeYML).bufferedReader().readLines()
            val nameLine = bungeeYMLContent.firstOrNull { it.startsWith("name:") }
            return@lazy nameLine?.split(":")?.get(1)?.trim() ?: "Unknown"
        }

        val velocityJSON = jarFile.getJarEntry("velocity-plugin.json")
        if (velocityJSON != null) {
            val velocityJSONContent = jarFile.getInputStream(velocityJSON).bufferedReader().readText()
            try {
                val nameLine = JSONObject(velocityJSONContent)["name"]
                return@lazy nameLine?.toString()?.trim() ?: "Unknown"
            } catch (e: Exception) {
                return@lazy "Unknown"
            }
        }
        return@lazy "Unknown"
    }

    fun uninstall() {
        file.delete()
    }
}