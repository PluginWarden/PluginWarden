package de.pluginwarden.data

enum class Availability {
    FOUND,
    NO_VALID_VERSION,
    INCOMPATIBLE,
    NOT_FOUND,
}

enum class Compatibility {
    COMPATIBLE,
    INCOMPATIBLE,
    WARNING
}