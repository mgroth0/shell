package matt.shell.commands.chmod

import matt.shell.Shell

fun <R> Shell<R>.chmod(vararg args: String): R = sendCommand(
    this::chmod.name,
    *args
)

fun <R> Shell<R>.chown(vararg args: String): R = sendCommand(
    this::chown.name,
    *args
)
