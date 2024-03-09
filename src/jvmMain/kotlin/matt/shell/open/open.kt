package matt.shell.open

import matt.shell.common.Shell


fun <R> Shell<R>.open(vararg args: String) =
    sendCommand(
        "open",
        *args
    )
