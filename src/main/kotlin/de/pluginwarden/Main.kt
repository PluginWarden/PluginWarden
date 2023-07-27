package de.pluginwarden

import de.pluginwarden.commands.ListCommand
import de.pluginwarden.commands.RemoveCommand

private val commands = mapOf(
    "list" to ListCommand,
    "remove" to RemoveCommand,
)

fun main(args: Array<String>) {
    if (args.isEmpty() || args[0] !in commands.keys) {
        println("No command specified!")
        println()
        println("Available commands:")
        commands.forEach { (name, _) ->
            println("  $name")
        }
        return
    }

    for (command in commands) {
        if (args[0] != command.key) continue
        command.value.execute(args.drop(1))
        return
    }
}
