package matt.shell.commands.sudo

import matt.shell.Shell
import matt.shell.context.ShellExecutionContext


fun <R> Shell<R>.sudo(
    op: Sudo<R>.() -> Unit
) = sudo.apply(op)

val <R> Shell<R>.sudo get() = Sudo(executionContext = executionContext, this)

class Sudo<R>(
    override val executionContext: ShellExecutionContext,
    private val shell: Shell<R>
) : Shell<R> {
    override fun sendCommand(vararg args: String) = shell.sendCommand("sudo", *args)
}
