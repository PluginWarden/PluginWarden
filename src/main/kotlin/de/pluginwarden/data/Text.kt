package de.pluginwarden.data

import org.fusesource.jansi.Ansi

class Text {

    private var length: Int = 0
    private var text: String = ""

    fun append(str: String): Text {
        length += str.length
        text += str
        return this
    }

    fun fg(color: Ansi.Color): Text {
        text += Ansi.ansi().fg(color).toString()
        return this
    }

    fun fgBright(color: Ansi.Color): Text {
        text += Ansi.ansi().fgBright(color).toString()
        return this
    }

    fun bg(color: Ansi.Color): Text {
        text += Ansi.ansi().bg(color).toString()
        return this
    }

    fun bgBright(color: Ansi.Color): Text {
        text += Ansi.ansi().bgBright(color).toString()
        return this
    }

    fun attribute(attribute: Ansi.Attribute): Text {
        text += Ansi.ansi().a(attribute).toString()
        return this
    }

    fun reset(): Text {
        text += Ansi.ansi().reset().toString()
        return this
    }

    fun newline(): Text {
        text += "\n"
        return this
    }

    fun length(): Int {
        return length
    }

    override fun toString(): String {
        return text
    }
}