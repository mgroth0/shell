package matt.shell.win

import matt.lang.common.unsafeErr
import matt.lang.model.file.AnyResolvableFilePath
import matt.lang.model.file.MacDefaultFileSystem
import matt.log.j.DefaultLogger
import matt.log.logger.Logger
import matt.model.code.sys.NewMac
import matt.prim.str.strings
import matt.shell.ShellVerbosity
import matt.shell.command
import matt.shell.commands.bash.bashC
import matt.shell.common.Shell
import matt.shell.commonj.context.DefaultWindowsExecutionContext
import matt.shell.commonj.context.ReapingShellExecutionContext
import matt.shell.shell

class WindowsGitBashReturner(
    override val executionContext: ReapingShellExecutionContext,
    private val verbosity: ShellVerbosity,
    val logger: Logger = DefaultLogger
) : Shell<String> {
    override val AnyResolvableFilePath.pathOp
        get() =
            NewMac.replaceFileSeparators(
                path,
                run {
                    unsafeErr("am I sure its not a removable filesystem of a different case-sensitivity?")
                    MacDefaultFileSystem
                }
            )

    override fun sendCommand(vararg args: String): String {
        with(executionContext) {
            return shell(
                *wrapWindowsBashCmd(*(args.strings())).asArray(),
                verbosity = verbosity,
                outLogger = logger,
                errLogger = logger
            )
        }
    }
}

fun wrapWindowsBashCmd(vararg command: String) =
    DefaultWindowsExecutionContext.command.bashC {

        sendCommand(*command)
    }
