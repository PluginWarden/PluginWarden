package de.pluginwarden.data

data class Version(val major: Int, val minor: Int, val patch: Int) : Comparable<Version> {

    override fun compareTo(other: Version): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        return patch.compareTo(other.patch)
    }

    override fun toString(): String {
        if (major == 0 && minor == 0 && patch == 0) return "Unknown"
        return "$major.$minor.$patch"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Version) return false

        if (major != other.major) return false
        if (minor != other.minor) return false
        return patch == other.patch
    }

    override fun hashCode(): Int {
        var result = major
        result = 31 * result + minor
        result = 31 * result + patch
        return result
    }
}
