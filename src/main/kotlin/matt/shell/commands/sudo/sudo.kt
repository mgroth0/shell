package matt.shell.commands.sudo

import matt.shell.Shell
import matt.shell.ShellProgramPathContext


val <R> Shell<R>.sudo get() = Sudo(programPathContext = programPathContext, this)

class Sudo<R>(
    override val programPathContext: ShellProgramPathContext,
    private val shell: Shell<R>
) : Shell<R> {
    override fun sendCommand(vararg args: String) = shell.sendCommand("sudo", *args)
}