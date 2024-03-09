package matt.shell.commands.curl

import matt.shell.common.Shell



fun <R> Shell<R>.curl(vararg args: String): R =
    sendCommand(
        ::curl.name,
        *args
    )
