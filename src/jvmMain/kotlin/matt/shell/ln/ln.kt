package matt.shell.ln

import matt.shell.common.Shell

fun <R> Shell<R>.ln(
    vararg args: String
) = sendCommand(
    "ln",
    *args
)
