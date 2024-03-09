package matt.shell.commands.debconf

import matt.shell.common.Shell


fun <R> Shell<R>.debconfSetSelections(vararg args: String) =
    sendCommand(
        "debconf-set-selections",
        *args
    )
