package matt.shell.commands.builtin

import matt.lang.model.file.AnyResolvableFilePath
import matt.shell.common.Shell

fun <R> Shell<R>.exit() = sendCommand("exit")


fun <R> Shell<R>.wait() = sendCommand("wait")


fun <R> Shell<R>.which(com: String): R =
    sendCommand(
        "which",
        com
    )


infix fun <R> Shell<R>.cd(dir: String) =
    sendCommand(
        "cd",
        dir
    )

infix fun <R> Shell<R>.cd(file: AnyResolvableFilePath): R = cd(file.path)

fun <R> Shell<R>.echo(vararg args: String) =
    sendCommand(
        "echo",
        *args
    )

fun <R> Shell<R>.pwd() = sendCommand("pwd")


fun <R> Shell<R>.printf(vararg args: String) =
    sendCommand(
        "printf",
        *args
    )


fun <R> Shell<R>.writeFile(
    filename: String,
    s: String
) = sendCommand(
    "echo \"${
        s.replace(
            "\"",
            "\\\""
        )
    }\" > \"$filename\""
)

fun <R> Shell<R>.writeFile(
    file: AnyResolvableFilePath,
    s: String
) = writeFile(
    filename = file.path,
    s = s
)






