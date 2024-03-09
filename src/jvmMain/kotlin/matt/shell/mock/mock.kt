package matt.shell.mock

import matt.shell.common.Shell
import matt.shell.common.context.ShellExecutionContext


object MockShell : Shell<Unit> {
    override fun sendCommand(vararg args: String) = Unit

    override val executionContext: ShellExecutionContext
        get() = TODO()
}
