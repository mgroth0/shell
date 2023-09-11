package matt.shell.commands.dnf

import matt.lang.If
import matt.shell.ControlledShellProgram
import matt.shell.Shell
import matt.shell.commands.apt.LinuxPackageManager
import matt.shell.commands.apt.aptget.AptGet

val <R> Shell<R>.microdnf get() = MicroDnf(this)


sealed interface Dnf : LinuxPackageManager

class MicroDnf<R>(shell: Shell<R>) : ControlledShellProgram<R>(program = "microdnf", shell = shell), Dnf {
    fun install(
        vararg packages: String,
        autoConfirm: Boolean = AptGet.DEFAULT_AUTO_CONFIRM, /*for some reason, autoconfirm seems to not be neccesary for microdnf?*/
    ) = sendCommand("install", *If(autoConfirm).then("-y"), *packages)
}

