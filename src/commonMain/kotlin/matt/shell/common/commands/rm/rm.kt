package matt.shell.common.commands.rm

import matt.lang.common.If
import matt.lang.model.file.AnyResolvableFilePath
import matt.shell.common.Shell
import matt.shell.common.commands.rm.RmOptions.Force
import matt.shell.common.commands.rm.RmOptions.Recursive
import matt.shell.common.context.shell.Powershell
import matt.shell.common.context.shell.UnixDirectCommands
import matt.shell.compile.compileSingleCharArguments

fun <R> Shell<R>.rm(
    file: AnyResolvableFilePath,
    rf: Boolean
): R = rm(file.path, rf = rf)

fun <R> Shell<R>.rm(
    file: String,
    rf: Boolean
): R =
    rm(
        file,
        options = if (rf) arrayOf(Recursive, Force) else arrayOf()
    )

fun <R> Shell<R>.rm(
    file: AnyResolvableFilePath,
    vararg options: RmOptions
): R =
    rm(
        file.path,
        options = options
    )


fun <R> Shell<R>.rm(
    filename: String,
    vararg options: RmOptions
): R =
    sendCommand(
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
            )
        ),
        filename
    )


enum class RmOptions {
    Recursive, Force /*also allows non-existing without a positive return code, at least on unix*/
}
