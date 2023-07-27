package de.pluginwarden.data

data class Version(val major: Int, val minor: Int, val patch: Int) {

    override fun toString(): String {
        if (major == 0 && minor == 0 && patch == 0) return "Unknown"
        return "$major.$minor.$patch"
    }
}
