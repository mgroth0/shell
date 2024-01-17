package matt.shell.mock

import matt.shell.Shell
import matt.shell.context.ShellExecutionContext


object MockShell : Shell<Unit> {
    override fun sendCommand(vararg args: String) = Unit

    override val executionContext: ShellExecutionContext
        get() = TODO()
}