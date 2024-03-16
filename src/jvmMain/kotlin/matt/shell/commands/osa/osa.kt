package matt.shell.commands.osa

import matt.lang.common.If
import matt.lang.model.file.AnyResolvableFilePath
import matt.shell.common.Shell

fun <R> Shell<R>.osascript(
    e: Boolean,
    vararg args: String
) = sendCommand(
    "osascript",
    *If(e).then("-e"),
    *args
)

fun <R> Shell<R>.osacompile(
    input: AnyResolvableFilePath,
    output: AnyResolvableFilePath
) = sendCommand(
    "osacompile",
    "-o",
    output.path,
    input.path
)
