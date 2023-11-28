package matt.shell.commands.rm

import matt.lang.If
import matt.lang.model.file.FilePath
import matt.shell.Shell
import matt.shell.commands.rm.RmOptions.Force
import matt.shell.commands.rm.RmOptions.Recursive
import matt.shell.compile.compileSingleCharArguments
import matt.shell.context.shell.Powershell
import matt.shell.context.shell.UnixDirectCommands

fun <R> Shell<R>.rm(
    file: FilePath,
    rf: Boolean
): R = rm(file.path, rf = rf)

fun <R> Shell<R>.rm(
    file: String,
    rf: Boolean
): R = rm(
    file,
    options = if (rf) arrayOf(Recursive, Force) else arrayOf()
)

fun <R> Shell<R>.rm(
    file: FilePath,
    vararg options: RmOptions
): R = rm(
    file.path,
    options = options
)


fun <R> Shell<R>.rm(
    filename: String,
    vararg options: RmOptions,
): R = sendCommand(
    "rm",
    *compileSingleCharArguments(
        *If(Recursive in options).then(
            "-r"
        ),
        *If(Force in options).then(
            when (executionContext.language) {
                is UnixDirectCommands -> "-f"
                is Powershell         -> "-Force"
            }
        ),
    ),
    filename
)


enum class RmOptions {
    Recursive, Force /*also allows non-existing without a positive return code, at least on unix*/
}