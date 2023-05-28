package matt.shell.commands.apt.addrep

import matt.shell.Shell

val <R> Shell<R>.aptAddRepository get() = AptAddRepository(this)

class AptAddRepository<R>(private val shell: Shell<R>) : Shell<R> {
    override fun sendCommand(vararg args: String) = shell.sendCommand("apt-add-repository", *args)
}

