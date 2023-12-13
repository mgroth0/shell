package matt.shell.commands.gpg

import matt.shell.Shell


fun <R> Shell<R>.gpg(vararg args: String) = sendCommand(
    "gpg",
    *args
)
