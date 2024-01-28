package matt.shell.win

import matt.lang.model.file.AnyResolvableFilePath
import matt.lang.model.file.MacFileSystem
import matt.lang.unsafeErr
import matt.log.DefaultLogger
import matt.log.logger.Logger
import matt.model.code.sys.NewMac
import matt.prim.str.strings
import matt.shell.Shell
import matt.shell.ShellVerbosity
import matt.shell.command
import matt.shell.commands.bash.bashC
import matt.shell.context.DefaultWindowsExecutionContext
import matt.shell.context.ReapingShellExecutionContext
import matt.shell.shell

class WindowsGitBashReturner(
    override val executionContext: ReapingShellExecutionContext,
    private val verbosity: ShellVerbosity,
    val logger: Logger = DefaultLogger,
) : Shell<String> {
    override val AnyResolvableFilePath.pathOp
        get() = NewMac.replaceFileSeparators(path, run {
            unsafeErr("am I sure its not a removable filesystem of a different case-sensitivity?")
            MacFileSystem
        })

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

//internal val WINDOWS_CMD_BASH_PREFIX = arrayOf("C:\\Program Files (x86)\\Git\\bin\\bash.exe", "-c")
fun wrapWindowsBashCmd(vararg command: String) = DefaultWindowsExecutionContext.command.bashC {

    sendCommand(*command)

//    arrayOf(*WINDOWS_CMD_BASH_PREFIX, command.joinToString(" "))
}
