package matt.shell.commands.apt.addrep

import matt.shell.common.Shell


val <R> Shell<R>.aptAddRepository get() = AptAddRepository(this)

class AptAddRepository<R>(private val shell: Shell<R>) : Shell<R> {
    override val executionContext = shell.executionContext
    override fun sendCommand(vararg args: String) = shell.sendCommand("apt-add-repository", *args)
}

