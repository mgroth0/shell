package matt.shell.commands.rm

import matt.lang.optArray
import matt.lang.model.file.FilePath
import matt.shell.Shell

fun <R> Shell<R>.rm(
    file: FilePath,
    rf: Boolean = false
) = rm(
    file.filePath,
    rf = rf
)


fun <R> Shell<R>.rm(
    filename: String,
    rf: Boolean = false,
    powershell: Boolean = false
) = sendCommand(
    "rm",
    *optArray(
        rf
    ) { if (powershell) arrayOf("-r", "-Force") else arrayOf("-rf") },
    filename
)
