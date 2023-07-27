@file:OptIn(ExperimentalCli::class)

package de.pluginwarden

import com.github.ajalt.mordant.terminal.Terminal
import de.pluginwarden.commands.ListCommand
import de.pluginwarden.commands.RemoveCommand
import kotlinx.cli.ArgParser
import kotlinx.cli.ExperimentalCli

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import de.pluginwarden.commands.InfoCommand

private val commands = listOf(
    ListCommand,
    RemoveCommand,
    InfoCommand
)

val t = Terminal()

fun main(args: Array<String>) {
    val parser = ArgParser("pluginwarden")
    parser.subcommands(*commands.toTypedArray())
    val result = parser.parse(args)
    if (result.commandName == "pluginwarden") {
        println(red("No command specified!"))
        println("Use ${bold("pluginwarden --help")} to get a list of all commands.")
        return
    }
}
