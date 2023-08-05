package de.pluginwarden.commands.install

import de.pluginwarden.data.StoragePluginVersion
import de.pluginwarden.data.Version
import de.pluginwarden.data.serverVersion
import de.pluginwarden.repository.getPluginStoragePlugin

val all = mutableSetOf<StoragePluginVersion>()
val resolved = mutableSetOf<String>()
val dependencies = mutableListOf<Pair<StoragePluginVersion, Pair<String, MutableList<Pair<String, Version>>>>>()
val dependenciesNotMet = mutableSetOf<StoragePluginVersion>()
val pluginIncompatibleWithOther = mutableSetOf<StoragePluginVersion>()

private fun Pair<String, Version>.toPluginVersion(): StoragePluginVersion? {
    return getPluginStoragePlugin(first)?.versions?.firstOrNull { it.version == second }
}

private fun StoragePluginVersion.isServerCompatible(): Boolean {
    return storagePluginServerVersions.filter { sv ->
        sv.serverType == de.pluginwarden.data.serverType || (de.pluginwarden.data.serverType != null && sv.serverType.isCompatibleWith(de.pluginwarden.data.serverType!!))
    }.any { sv ->
        sv.compatibilityChecker(serverVersion!!).first
    }
}

fun add(storagePluginVersion: StoragePluginVersion) {
    all.add(storagePluginVersion)

    dependencies.removeIf {
        if (dependenciesNotMet.contains(it.first)) {
            return@removeIf true
        }

        val allDependencies = it.first.storagePluginDependencies.flatMap { it.dependencies }.toMutableList()
        val currentDependencies = storagePluginVersion.storagePluginDependencies.flatMap { it.dependencies }.toMutableList()
        if (allDependencies.any { currentDependencies.contains(it) }) {
            allDependencies.retainAll(currentDependencies)
            it.second.second.retainAll(allDependencies)
        }

        it.second.second.removeIf {
            it.first == storagePluginVersion.name && it.second != storagePluginVersion.version
        }
        if (it.second.second.isEmpty()) {
            dependenciesNotMet.add(it.first)
        }
        it.second.second.isEmpty()
    }

    storagePluginVersion.storagePluginDependencies.forEach {
        dependencies.add(storagePluginVersion to (it.groupName to it.dependencies.filter { it.toPluginVersion()?.isServerCompatible() ?: false }.toMutableList()))
    }
}

fun resolve(): Boolean {
    var changed = false
    dependencies.filter { it.second.second.size == 1 }
        .filter { resolved.add("${it.first.name}:${it.first.version}->${it.second.first}") }
        .flatMap { it.second.second }
        .forEach {
            val storagePluginVersion = getPluginStoragePlugin(it.first)
                ?: return@forEach
            add(storagePluginVersion.versions.firstOrNull { version -> version.version == it.second }
                ?: return@forEach)
            changed = true
        }
    return changed
}

fun dependencyChoiceIncompatible() {
    dependencies.filter { it.second.second.size == 1 }.forEach {
        it.second.second.first()
        println("${it.first.name}:${it.first.version}")
    }
}

fun pluginIncompatible() {
    all.filter { !dependenciesNotMet.contains(it) }.forEach {
        val incompatible = it.storagePluginIncompatibilities.any { compatibility ->
            all.any {
                compatibility.pluginName == it.name && compatibility.versionChecker(it.version).first
            }
        }
        if (incompatible) {
            pluginIncompatibleWithOther.add(it)
        }
    }
}