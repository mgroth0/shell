package matt.shell.commands.sudo

import matt.shell.Shell


val <R> Shell<R>.sudo get() = Sudo(this)

class Sudo<R>(private val shell: Shell<R>) : Shell<R> {
    override fun sendCommand(vararg args: String) = shell.sendCommand("sudo", *args)
}