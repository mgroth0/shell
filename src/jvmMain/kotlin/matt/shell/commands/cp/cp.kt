package matt.shell.commands.cp

import matt.lang.If
import matt.lang.anno.SeeURL
import matt.lang.model.file.AnyFsFile
import matt.shell.Shell


@SeeURL("https://unix.stackexchange.com/questions/228597/how-to-copy-a-folder-recursively-in-an-idempotent-way-using-cp")
fun <R> Shell<R>.cp(
    from: AnyFsFile,
    to: AnyFsFile,
    recursive: Boolean = false,
    force: Boolean = false,
    useDirIdempotencyTrick: Boolean = false
) = sendCommand(
    "cp",
    *If(
        recursive
    ).then(
        "-R"
    ), /*man page says to never use -r (lowercase), it is basically DEPRECATED*/
    *If(
        force
    ).then(
        "-f"
    ),


    if (useDirIdempotencyTrick) from.path.removeSuffix(
        from.partSep
    ) + from.partSep + "." /*https://unix.stackexchange.com/questions/711921/how-to-cp-with-no-target-directory-on-mac?noredirect=1#comment1348202_711921*/
    else from.pathOp, /*using "path/path/." with a file leads to an error. But whether this will always hit the target correctly with a file is undefined... */
    to.pathOp
)
