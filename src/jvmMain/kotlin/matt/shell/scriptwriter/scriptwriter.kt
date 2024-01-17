package matt.shell.scriptwriter

import kotlinx.serialization.json.internal.FormatLanguage
import matt.model.code.CodeGenerator
import matt.model.code.SimpleFormatCode
import matt.prim.str.joinWithSpaces
import matt.shell.Shell
import matt.shell.context.escape.EscapeStrategy

interface ScriptWriterContext : Shell<Unit> {
    fun addRawLines(vararg lines: String, index: Int? = null)
}



abstract class ScriptWriter(
    private val escapeStrategy: EscapeStrategy,
) : ScriptWriterContext, CodeGenerator<ShellScript> {


    /*private val escapeStrategy: EscapeStrategy,*/

    private val scriptLinesM = mutableListOf<String>()
    val scriptLines: List<String> = scriptLinesM


    override fun sendCommand(vararg args: String) {
        scriptLinesM += args.joinWithSpaces {
            escapeStrategy.escape(it)
        }
    }


    override fun addRawLines(vararg lines: String, index: Int?) {
        if (index != null) {
            scriptLinesM.addAll(0, lines.toList())
        } else scriptLinesM.addAll(lines)
    }


    private var lineDelimiter = "\n"

    val script get() = scriptLinesM.joinToString(separator = lineDelimiter) { it }


    override fun generate(): ShellScript {
        return ShellScript(script)
    }
}



class ShellScript(@FormatLanguage("Shell Script", "", "")  override val code: String) : SimpleFormatCode<ShellScript> {
    override fun formatted(): ShellScript {
        TODO()
    }
}
