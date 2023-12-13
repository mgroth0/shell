package matt.shell.commands.curl

import matt.shell.Shell



fun <R> Shell<R>.curl(vararg args: String): R = sendCommand(
    this::curl.name,
    *args
)