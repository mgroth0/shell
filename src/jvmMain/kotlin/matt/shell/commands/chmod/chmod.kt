package matt.shell.commands.chmod

import matt.shell.common.Shell

fun <R> Shell<R>.chmod(vararg args: String): R =
    sendCommand(
        ::chmod.name,
        *args
    )

fun <R> Shell<R>.chown(vararg args: String): R =
    sendCommand(
        ::chown.name,
        *args
    )
