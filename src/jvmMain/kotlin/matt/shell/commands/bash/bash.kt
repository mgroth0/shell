package matt.shell.commands.bash

import matt.lang.anno.optin.IncubatingMattCode
import matt.lang.context.ShellProgramPathContext.Windows
import matt.shell.Shell
import matt.shell.context.ShellExecutionContext
import matt.shell.context.escape.NoEscaping
import matt.shell.context.escape.unix.UnixEscapeDoubleQuotedStringContext
import matt.shell.scriptwriter.unix.bash.BashWriter
import matt.shell.scriptwriter.unix.bash.SANE_BASH_CONFIG_DEFAULT

fun <R> Shell<R>.bash(vararg args: String): R = sendCommand(
    when (this.executionContext.shellProgramPathContext) {
        Windows -> "C:\\Program Files (x86)\\Git\\bin\\bash.exe"
        else    -> this::bash.name
    },
    * args
)


@OptIn(IncubatingMattCode::class)
fun <R> Shell<R>.bashC(
    execContext: ShellExecutionContext = executionContext,
    saneBashConfig: Boolean = SANE_BASH_CONFIG_DEFAULT,
    putInDoubleQuotes: Boolean = false,
    op: BashWriter.() -> Unit
) = bash(
    "-c",
    BashWriter(
        executionContext = execContext,
        escapeStrategy =
        when {

            putInDoubleQuotes -> UnixEscapeDoubleQuotedStringContext
            /*If I use UnixEscapeDoubleQuotedStringContext, spaces are escaped, causing the redis setup script to fail because openssl req arg has spaces...*/
            /*putInDoubleQuotes -> UnixEscapeUnquotedStringContext*/
            /*execContext.argumentsAreSeparated ?: error("need to know if arguments are separated") -> NoEscaping*/
            else              -> NoEscaping /*UnixEscapeUnquotedStringContext*/
        }
            /*(if (putInDoubleQuotes) UnixEscapeDoubleQuotedStringContext else UnixEscapeUnquotedStringContext)*/.escapeWithEscapeChar(),
        saneBashConfig = saneBashConfig
    ).apply(op).script.let {
        if (putInDoubleQuotes) "\"$it\"" else it
    }
)