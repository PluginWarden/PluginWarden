package de.pluginwarden.commands.install

import de.pluginwarden.data.StoragePluginVersion

private typealias PluginWithVersion = Pair<String, String>

class Graph {
    private val edges = mutableListOf<Pair<PluginWithVersion, List<PluginWithVersion>>>()

    fun add(storagePluginVersion: StoragePluginVersion) {
        val pluginVersion: PluginWithVersion = storagePluginVersion.name to storagePluginVersion.version.toString()
        storagePluginVersion.storagePluginDependencies.forEach {
            edges.add(pluginVersion to (it.dependencies.map { it.first to it.second.toString() }))
        }
    }

    fun getNoDependencies(): List<Pair<PluginWithVersion, List<PluginWithVersion>>> {
        return edges.filter { it.second.isEmpty() }
    }

    fun getNoChoiceDependencies(): List<Pair<PluginWithVersion, List<PluginWithVersion>>> {
        return edges.filter { it.second.size == 1 }
    }

    fun getChoiceDependencies(): List<Pair<PluginWithVersion, List<PluginWithVersion>>> {
        return edges.filter { it.second.size > 1 }
    }
}
