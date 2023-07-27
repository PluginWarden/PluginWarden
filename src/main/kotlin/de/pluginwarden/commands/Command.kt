package de.pluginwarden.commands

interface Command {
    fun execute(args: List<String>)
}