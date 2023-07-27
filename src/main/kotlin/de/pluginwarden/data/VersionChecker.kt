package de.pluginwarden.data

// 0.0.0
// 0.0.?
// 0.?
// ?
// >0.0.0
// <0.0.0
// 0.0.0-0.0.0
// 0.0.0-0.0.?
// 0.0.0-0.?
// 0.0.?-0.0.0
// 0.0.?-0.0.?
// 0.0.?-0.*
// 0.?-0.0.0
// 0.?-0.0.?
// 0.?-0.?
// 0.0.0,0.1.0
fun String.toVersionChecker(): (Version) -> Pair<Boolean, Boolean> {
    val subVersionChecks = split(",").map { it.trim() }
    val predicates = mutableListOf<(Version) -> Pair<Boolean, Boolean>>()

    subVersionChecks.forEach {
        var warning = false
        val it = if (it.endsWith("!")) {
            warning = true
            it.substring(0, it.length - 1)
        } else {
            it
        }

        if (it.startsWith(">")) {
            val checkedAgainst = it.substring(1).toVersion()
            predicates.add { version ->
                (version >= checkedAgainst) to warning
            }
        } else if (it.endsWith("<")) {
            val checkedAgainst = it.substring(1).toVersion()
            predicates.add { version ->
                (version <= checkedAgainst) to warning
            }
        } else if (it.contains("-")) {
            val (min, max) = it.split("-")
            val minFuzzy = min.toFuzzyVersion()
            val maxFuzzy = max.toFuzzyVersion()
            predicates.add { version ->
                (version.toFuzzy() in minFuzzy..maxFuzzy) to warning
            }
        } else {
            val checkedAgainst = it.toFuzzyVersion()
            predicates.add { version ->
                (version.toFuzzy() == checkedAgainst) to warning
            }
        }
    }

    return predicate@ { version ->
        predicates.forEach {
            val (result, warning) = it(version)
            if (result) return@predicate true to warning
        }
        return@predicate false to false
    }
}

data class FuzzyVersion(val major: Int?, val minor: Int?, val patch: Int?): Comparable<FuzzyVersion> {

    override fun compareTo(other: FuzzyVersion): Int {
        if (major == null) return 0
        if (major != other.major) return major.compareTo(other.major!!)
        if (minor == null) return 0
        if (minor != other.minor) return minor.compareTo(other.minor!!)
        if (patch == null) return 0
        return patch.compareTo(other.patch!!)
    }
}

private fun Version.toFuzzy(): FuzzyVersion {
    return FuzzyVersion(major, minor, patch)
}

private val fullVersionRegex = "(\\d+)\\.(\\d+)\\.(\\d+)".toRegex()
private val ignorePatchVersionRegex = "(\\d+)\\.(\\d+)\\.\\?".toRegex()
private val ignoreMinorVersionRegex = "(\\d+)\\.?".toRegex()
private val ignoreMajorVersionRegex = "\\?".toRegex()

private fun String.toFuzzyVersion(): FuzzyVersion {
    fullVersionRegex.find(this)?.let {
        return FuzzyVersion(it.groups[1]!!.value.toInt(), it.groups[2]!!.value.toInt(), it.groups[3]!!.value.toInt())
    }
    ignorePatchVersionRegex.find(this)?.let {
        return FuzzyVersion(it.groups[1]!!.value.toInt(), it.groups[2]!!.value.toInt(), null)
    }
    ignoreMinorVersionRegex.find(this)?.let {
        return FuzzyVersion(it.groups[1]!!.value.toInt(), null, null)
    }
    ignoreMajorVersionRegex.find(this)?.let {
        return FuzzyVersion(null, null, null)
    }
    throw IllegalArgumentException("Invalid fuzzy version string: $this")
}