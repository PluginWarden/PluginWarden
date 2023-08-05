@file:OptIn(ExperimentalCli::class)

package de.pluginwarden

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.cli.ArgParser
import kotlinx.cli.ExperimentalCli

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import de.pluginwarden.commands.*
import org.fusesource.jansi.AnsiConsole

private val commands = listOf(
    TempCommand,
    ListCommand,
    RemoveCommand,
    InfoCommand,
    SearchCommand,
    InstallCommand,
    UninstallCommand
)

val windows = System.getProperty("os.name").contains("windows", true)

val t = if (windows) {
    Terminal(AnsiLevel.TRUECOLOR)
} else {
    Terminal()
}

fun main(args: Array<String>) {
    if (windows) {
        AnsiConsole.systemInstall()
        Runtime.getRuntime().addShutdownHook(Thread(AnsiConsole::systemUninstall))
    }

    val parser = ArgParser("pluginwarden")
    parser.subcommands(*commands.toTypedArray())
    val result = parser.parse(args)
    if (result.commandName == "pluginwarden") {
        println(red("No command specified!"))
        println("Use ${bold("pluginwarden --help")} to get a list of all commands.")
        return
    }
}
