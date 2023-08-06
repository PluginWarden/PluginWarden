package de.pluginwarden.data

import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import java.io.File

class StoragePlugin(val file: File) {

    val name = file.name

    val prefixes: List<String> by lazy {
        val versionFile = File(file, ".version")
        if (!versionFile.exists()) return@lazy emptyList<String>()
        versionFile.readLines()
    }

    val versions: List<StoragePluginVersion> by lazy {
        file.listFiles { _, name -> name.endsWith(".md") }
            ?.map { StoragePluginVersion(it, this) }
            ?.sortedByDescending { it.version }
            ?: emptyList()
    }
}

val flavour = CommonMarkFlavourDescriptor()
val parser = MarkdownParser(flavour)

class StoragePluginVersion(file: File, val plugin: StoragePlugin) {

    val name: String = file.parentFile.name!!
    val version = file.nameWithoutExtension.toVersion()

    val storagePluginDownloads: MutableList<StoragePluginDownload>
    val storagePluginServerVersions: MutableList<StoragePluginServerVersion>
    val storagePluginIncompatibilities: MutableList<StoragePluginIncompatibility>
    val storagePluginDependencies: MutableList<StoragePluginDependency>

    init {
        val content = file.bufferedReader().readText()

        val ast = parser.buildMarkdownTreeFromString(content)

        storagePluginDownloads = mutableListOf()
        storagePluginServerVersions = mutableListOf()
        storagePluginIncompatibilities = mutableListOf()
        storagePluginDependencies = mutableListOf()

        var state = ParserState.STARTING

        fun getHeaderName(node: ASTNode) = node.children.first { it.type.name == "ATX_CONTENT" }.getTextInNode(content).trim()

        fun parseDependency(node: ASTNode) {
            var list: ASTNode? = null
            var par: CharSequence = ""

            node.children.forEach { depNode ->
                if (depNode.type.name == "PARAGRAPH") {
                    par = depNode.getTextInNode(content)
                } else if(depNode.type.name == "UNORDERED_LIST") {
                    list = depNode
                }
            }
            if (list == null) {
                val args = par.split(": ")
                storagePluginDependencies.add(StoragePluginDependency(args[0], listOf(Pair(args[0], args[1].toVersion()))))
            } else {
                val pairList = mutableListOf<Pair<String, Version>>()
                list!!.children.forEach {li ->
                    li.children.forEach { pars ->
                        if(pars.type.name == "PARAGRAPH") {
                            val args = pars.getTextInNode(content).split(": ")
                            pairList.add(args[0] to args[1].toVersion())
                        }
                    }
                }
                storagePluginDependencies.add(StoragePluginDependency(par.toString(), pairList))
            }
        }

        fun parseList(node: ASTNode) {
            node.children.forEach {
                if (it.children.size < 2) return@forEach
                val value = it.children[1]
                when(state) {
                    ParserState.DOWNLOAD -> {
                        val args = value.getTextInNode(content).split(": ")
                        var arg = args.last().trim()
                        var file: String? = null
                        val serverType = if (args.size == 1) null else ServerType.byName(args[0])
                        if (arg.contains(" -> ")) {
                            val split = arg.split(" -> ")
                            arg = split[0]
                            file = split[1]
                        }
                        storagePluginDownloads.add(StoragePluginDownload(serverType, Pair(arg, file)))
                    }
                    ParserState.SERVER -> {
                        val args = value.getTextInNode(content).split(": ")
                        storagePluginServerVersions.add(StoragePluginServerVersion(ServerType.byName(args[0]), args[1].toVersionChecker()))
                    }
                    ParserState.INCOMPATIBLE -> {
                        val args = value.getTextInNode(content).split(": ")
                        storagePluginIncompatibilities.add(StoragePluginIncompatibility(args[0], args[1].toVersionChecker()))
                    }
                    ParserState.DEPENDENCIES -> parseDependency(it)

                    else -> {

                    }
                }
            }
        }

        ast.children.forEach {
            if(it.type.name == "ATX_2") {
                try {
                    val name = getHeaderName(it)
                    state = ParserState.values().first { i -> i.v == name }
                } catch (e: Exception) {
                    // ignore
                }
            } else if(it.type.name == "UNORDERED_LIST") {
                parseList(it)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StoragePluginVersion) return false

        if (name != other.name) return false
        if (version != other.version) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + version.hashCode()
        return result
    }
}

data class StoragePluginDownload(val serverType: ServerType?, val link: Pair<String, String?>)

data class StoragePluginServerVersion(val serverType: ServerType, val compatibilityChecker: (Version) -> Pair<Boolean, Boolean>)

data class StoragePluginIncompatibility(val pluginName: String, val versionChecker: (Version) -> Pair<Boolean, Boolean>)

data class StoragePluginDependency(val groupName: String, val dependencies: List<Pair<String, Version>>)

private enum class ParserState(val v: String) {
    STARTING(""),
    DOWNLOAD("Download"),
    SERVER("Server versions"),
    INCOMPATIBLE("Incompatible with"),
    DEPENDENCIES("Dependencies");
}