package matt.shell.commands.apt

import matt.lang.If
import matt.shell.ControlledShellProgram
import matt.shell.Shell
import matt.shell.commands.apt.aptget.AptGet.Companion.DEFAULT_AUTO_CONFIRM
import matt.shell.commands.apt.aptget.AptGet.Companion.DEFAULT_AUTO_REMOVE
import matt.shell.commands.apt.aptget.AptGet.Companion.DEFAULT_FIX_MISSING

interface LinuxPackageManager
interface AptLike : LinuxPackageManager

val <R> Shell<R>.apt get() = Apt(this)

class Apt<R>(shell: Shell<R>) : ControlledShellProgram<R>(program = "apt", shell = shell), AptLike {
    fun update() = sendCommand("update")
    fun install(
        vararg packages: String,
        autoConfirm: Boolean = DEFAULT_AUTO_CONFIRM,
        fixMissing: Boolean = DEFAULT_FIX_MISSING
    ) = sendCommand(
        "install",
        *If(autoConfirm).then("-y"),
        *If(fixMissing).then("--fix-missing"),
        * packages
    )


    fun remove(
        vararg packages: String,
        autoRemove: Boolean = DEFAULT_AUTO_REMOVE
    ) = sendCommand(
        "remove",
        *If(autoRemove).then("--autoremove"),
        *packages
    )

    fun purge(
        vararg packages: String,
        autoRemove: Boolean = DEFAULT_AUTO_REMOVE,
        autoConfirm: Boolean = DEFAULT_AUTO_CONFIRM,
    ) = sendCommand(
        "purge",
        *If(autoRemove).then("--autoremove"),
        *If(autoConfirm).then("-y"),
        *packages
    )

    fun listInstalled() = sendCommand("list", "--installed")
}



