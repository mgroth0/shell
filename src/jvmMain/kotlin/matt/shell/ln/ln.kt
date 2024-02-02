package matt.shell.ln

import matt.shell.Shell

fun <R> Shell<R>.ln(
    vararg args: String
) = sendCommand(
    "ln",
    *args
)
