package matt.shell.commands.brew

import matt.shell.Shell

fun <R> Shell<R>.brew(vararg args: String): R = sendCommand(
    this::brew.name,
    *args
)