package matt.shell.commands.apt.key

import matt.shell.ControlledShellProgram
import matt.shell.Shell

val <R> Shell<R>.aptKey get() = AptKey(this)

class AptKey<R>(shell: Shell<R>) : ControlledShellProgram<R>(shell = shell, program = "apt-key") {
    fun del(key: String) = sendCommand("del", key)
    fun adv(vararg args: String) = sendCommand("adv", *args)
}
