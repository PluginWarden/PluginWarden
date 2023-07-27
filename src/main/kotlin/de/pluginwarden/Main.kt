package de.pluginwarden

import de.pluginwarden.commands.InfoCommand
import de.pluginwarden.commands.ListCommand
import de.pluginwarden.commands.RemoveCommand
import org.fusesource.jansi.AnsiConsole

private val commands = mapOf(
    "info" to InfoCommand,
    "list" to ListCommand,
    "remove" to RemoveCommand,
)

fun main(args: Array<String>) {
    AnsiConsole.systemInstall()
    Runtime.getRuntime().addShutdownHook(Thread {
        AnsiConsole.systemUninstall()
    })

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
