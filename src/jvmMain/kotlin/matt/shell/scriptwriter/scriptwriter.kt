package matt.shell.scriptwriter

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.internal.FormatLanguage
import matt.model.code.CodeGenerator
import matt.model.code.SimpleFormatCode
import matt.prim.str.joinWithSpaces
import matt.shell.common.Shell
import matt.shell.commonj.context.escape.EscapeStrategy

interface ScriptWriterContext : Shell<Unit> {
    fun addRawLines(vararg lines: String, index: Int? = null)
}



abstract class ScriptWriter(
    private val escapeStrategy: EscapeStrategy
) : ScriptWriterContext, CodeGenerator<ShellScript> {


    /*private val escapeStrategy: EscapeStrategy,*/

    private val scriptLinesM = mutableListOf<String>()
    val scriptLines: List<String> = scriptLinesM


    final override fun sendCommand(vararg args: String) {
        scriptLinesM +=
            args.joinWithSpaces {
                escapeStrategy.escape(it)
            }
    }


    final override fun addRawLines(vararg lines: String, index: Int?) {
        if (index != null) {
            scriptLinesM.addAll(0, lines.toList())
        } else scriptLinesM.addAll(lines)
    }


    private val lineDelimiter = "\n"

    val script get() = scriptLinesM.joinToString(separator = lineDelimiter) { it }


    final override fun generate(): ShellScript = ShellScript(script)
}



class ShellScript @OptIn(InternalSerializationApi::class) constructor(
    @FormatLanguage("Shell Script", "", "") override val code: String
) : SimpleFormatCode<ShellScript> {
    override fun formatted(): ShellScript {
        TODO()
    }
}
