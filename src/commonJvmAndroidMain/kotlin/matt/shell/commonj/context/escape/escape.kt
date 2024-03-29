package matt.shell.commonj.context.escape

import matt.lang.anno.Open
import matt.lang.common.NEVER
import matt.prim.str.mybuild.api.string

sealed interface EscapeStrategy {
    fun escape(s: String): String
}

data object None : EscapeStrategy {
    override fun escape(s: String): String = s
}

class EscapeWithQuotes internal constructor(private val escapeContext: EscapeContext) : EscapeStrategy {
    override fun escape(s: String): String = if (escapeContext.charsToEscape.any { it in s }) "\"$s\"" else s
}


class EscapeWithEscapeChar internal constructor(private val escapeContext: EscapeContext) : EscapeStrategy {
    fun escapeEach(vararg s: String) = s.map { escape(it) }
    fun escapeEach(s: List<String>) = s.map { escape(it) }
    override fun escape(s: String): String =
        string {
            s.forEach {
                if (it in escapeContext.charsToEscape) append(escapeContext.escapeChar)
                append(it)
            }
        }
}

class EscapeWithEscapeCharAndEscapeNewlines internal constructor(private val escapeContext: EscapeContext) :
    EscapeStrategy {
        private val baseEscape = escapeContext.escapeWithEscapeChar()
        override fun escape(s: String): String {
            val r1 = baseEscape.escape(s)
            return string {
                r1.forEach {
                    if (it == '\n') {
                        println("escaping newline")
                        append("\\n")
                    } else {
                        append(it)
                    }
                }
            }
        }
    }

interface EscapeContext {
    val charsToEscape: List<Char>
    val escapeChar: Char

    @Open
    fun escapeWithQuotes() = EscapeWithQuotes(this)

    @Open
    fun escapeWithEscapeChar() = EscapeWithEscapeChar(this)

    @Open
    fun escapeWithEscapeCharAndEscapeNewlines() = EscapeWithEscapeCharAndEscapeNewlines(this)
}

object NoEscaping : EscapeContext {
    override val charsToEscape = emptyList<Char>()
    override val escapeChar: Char
        get() = NEVER
}

