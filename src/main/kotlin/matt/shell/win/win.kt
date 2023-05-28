package matt.shell.win

import matt.log.DefaultLogger
import matt.log.logger.Logger
import matt.model.code.sys.NEW_MAC
import matt.model.data.file.FilePath
import matt.prim.str.strings
import matt.shell.Shell
import matt.shell.ShellVerbosity
import matt.shell.shell

class WindowsGitBashReturner(private val verbosity: ShellVerbosity, val logger: Logger = DefaultLogger):
    Shell<String> {
  override val FilePath.pathOp get() = NEW_MAC.replaceFileSeparators(filePath)
  override fun sendCommand(vararg args: String): String {
	return shell(*wrapWindowsBashCmd(*(args.strings())), verbosity = verbosity, logger = logger)
  }
}

internal val WINDOWS_CMD_BASH_PREFIX = arrayOf("C:\\Program Files (x86)\\Git\\bin\\bash.exe", "-c")
fun wrapWindowsBashCmd(vararg command: String) = arrayOf(*WINDOWS_CMD_BASH_PREFIX, command.joinToString(" "))