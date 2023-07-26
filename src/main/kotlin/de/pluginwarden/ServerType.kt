package de.pluginwarden

import java.io.File

sealed interface ServerType {

    companion object {

        fun fromFile(pwd: File): Pair<ServerType, File>? {
            Spigot.isServerType(pwd).let { return Pair(Spigot, it) }
            Paper.isServerType(pwd).let { return Pair(Paper, it) }
            BungeeCord.isServerType(pwd).let { return Pair(BungeeCord, it) }
            Waterfall.isServerType(pwd).let { return Pair(Waterfall, it) }
            Velocity.isServerType(pwd).let { return Pair(Velocity, it) }
            return null
        }
    }

    fun isServerType(pwd: File): File
    fun getVersion(file: File): Version
}

object Spigot: ServerType {

    private val versionRegex = Regex("spigot-(?<MAJOR>\\d+)\\.(?<MINOR>\\d+)\\.(?<PATCH>\\d+)\\.jar")

    override fun isServerType(pwd: File): File {
        return pwd.listFiles { dir, name -> name.startsWith("spigot") && name.endsWith(".jar") }.first()
    }

    override fun getVersion(file: File): Version {
        versionRegex.find(file.name)?.let {
            return Version(it.groups["MAJOR"]!!.value.toInt(), it.groups["MINOR"]!!.value.toInt(), it.groups["PATCH"]!!.value.toInt())
        }
        // TODO: Add jar file parsing
        return Version(0, 0, 0)
    }
}

object Paper: ServerType {

    private val versionRegex = Regex("paper-(?<MAJOR>\\d+)\\.(?<MINOR>\\d+)\\.(?<PATCH>\\d+).*?\\.jar")

    override fun isServerType(pwd: File): File {
        return pwd.listFiles { dir, name -> name.startsWith("paper") && name.endsWith(".jar") }.first()
    }

    override fun getVersion(file: File): Version {
        versionRegex.find(file.name)?.let {
            return Version(it.groups["MAJOR"]!!.value.toInt(), it.groups["MINOR"]!!.value.toInt(), it.groups["PATCH"]!!.value.toInt())
        }
        // TODO: Add jar file parsing
        return Version(0, 0, 0)
    }
}

object BungeeCord: ServerType {

    override fun isServerType(pwd: File): File {
        return pwd.listFiles { dir, name -> name.startsWith("BungeeCord") && name.endsWith(".jar") }.first()
    }

    override fun getVersion(file: File): Version {
        return Version(0, 0, 0)
    }
}

object Waterfall: ServerType {

    private val versionRegex = Regex("waterfall-(?<MAJOR>\\d+)\\.(?<MINOR>\\d+)(\\.(?<PATCH>\\d+))?.*?\\.jar")

    override fun isServerType(pwd: File): File {
        return pwd.listFiles { dir, name -> name.startsWith("waterfall") && name.endsWith(".jar") }.first()
    }

    override fun getVersion(file: File): Version {
        versionRegex.find(file.name)?.let {
            return Version(it.groups["MAJOR"]!!.value.toInt(), it.groups["MINOR"]!!.value.toInt(), it.groups["PATCH"]?.value?.toInt() ?: 0)
        }
        // TODO: Add jar file parsing
        return Version(0, 0, 0)
    }
}

object Velocity: ServerType {

    private val versionRegex = Regex("velocity-(?<MAJOR>\\d+)\\.(?<MINOR>\\d+)\\.(?<PATCH>\\d+).*?\\.jar")

    override fun isServerType(pwd: File): File {
        return pwd.listFiles { dir, name -> name.startsWith("velocity") && name.endsWith(".jar") }.first()
    }

    override fun getVersion(file: File): Version {
        versionRegex.find(file.name)?.let {
            return Version(it.groups["MAJOR"]!!.value.toInt(), it.groups["MINOR"]!!.value.toInt(), it.groups["PATCH"]!!.value.toInt())
        }
        // TODO: Add jar file parsing
        return Version(0, 0, 0)
    }
}
