package matt.shell.commands.mkdir

import matt.lang.If
import matt.lang.model.file.AnyResolvableFilePath
import matt.shell.Shell


fun <R> Shell<R>.mkdir(
    name: String,

    /*-p: no error if existing, make parent directories as needed,
              with their file modes unaffected by any -m option*/
    p: Boolean = false
) = sendCommand(
    "mkdir",
    *If(
        p
    ).then(
        "-p"
    ),
    name
)


fun <R> Shell<R>.mkdir(
    file: AnyResolvableFilePath,
    p: Boolean = false
) = apply {
    mkdir(
        file.path,
        p = p
    )
}
