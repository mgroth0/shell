package matt.shell.commands.apt.addrep

import matt.shell.DEFAULT_LINUX_PROGRAM_PATH_CONTEXT
import matt.shell.Shell
import matt.shell.ShellProgramPathContext

val <R> Shell<R>.aptAddRepository get() = AptAddRepository(this)

class AptAddRepository<R>(private val shell: Shell<R>) : Shell<R> {
    override val programPathContext: ShellProgramPathContext = DEFAULT_LINUX_PROGRAM_PATH_CONTEXT
    override fun sendCommand(vararg args: String) = shell.sendCommand("apt-add-repository", *args)
}

