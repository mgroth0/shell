package matt.shell.commands.dnf

import matt.lang.common.If
import matt.shell.ControlledShellProgram
import matt.shell.commands.apt.LinuxPackageManager
import matt.shell.commands.apt.aptget.AptGet
import matt.shell.common.Shell

val <R> Shell<R>.microdnf get() = MicroDnf(this)


sealed interface Dnf : LinuxPackageManager

class MicroDnf<R>(shell: Shell<R>) : ControlledShellProgram<R>(program = "microdnf", shell = shell), Dnf {
    fun install(
        vararg packages: String,
        autoConfirm: Boolean = AptGet.DEFAULT_AUTO_CONFIRM /*for some reason, auto-confirm seems to not be neccesary for microdnf?*/
    ) = sendCommand("install", *If(autoConfirm).then("-y"), *packages)
}

