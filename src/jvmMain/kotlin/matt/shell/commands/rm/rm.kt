package matt.shell.commands.rm

import matt.lang.model.file.FilePath
import matt.lang.optArray
import matt.shell.Shell

fun <R> Shell<R>.rm(
    file: FilePath,
    rf: Boolean = false
): R = rm(
    file.path,
    rf = rf
)


fun <R> Shell<R>.rm(
    filename: String,
    rf: Boolean = false,
    powershell: Boolean = false
): R = sendCommand(
    "rm",
    *optArray(
        rf
    ) { if (powershell) arrayOf("-r", "-Force") else arrayOf("-rf") },
    filename
)
