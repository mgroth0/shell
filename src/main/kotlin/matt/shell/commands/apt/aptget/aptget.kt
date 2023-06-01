package matt.shell.commands.apt.aptget

import matt.lang.If
import matt.log.warn.warn
import matt.shell.ControlledShellProgram
import matt.shell.Shell
import matt.shell.commands.apt.AptLike

val <R> Shell<R>.aptGet get() = AptGet(this)

class AptGet<R>(shell: Shell<R>) : ControlledShellProgram<R>(shell = shell, program = "apt-get"), AptLike {

    companion object {
        const val DEFAULT_AUTO_CONFIRM = false
        internal const val DEFAULT_AUTO_REMOVE = false
    }

    fun update() = sendCommand("update")

    fun install(
        vararg packages: String,
        autoConfirm: Boolean = DEFAULT_AUTO_CONFIRM,
        reinstall: Boolean = false,
        options: List<String> = listOf()
    ) {
        sendCommand(
            "install",
            *If(autoConfirm).then("-y"),
            *If(reinstall).then("--reinstall"),
            *options.flatMap { listOf("-o", it) }.toTypedArray(),
            * packages
        )
    }

    fun remove(
        vararg packages: String,
        autoConfirm: Boolean = DEFAULT_AUTO_CONFIRM,
        autoRemove: Boolean = DEFAULT_AUTO_REMOVE
    ) {
        warn("Are you sure you do not want to use purge?")
        sendCommand(
            "remove",
            *If(autoConfirm).then("-y"),
            *If(autoRemove).then("--autoremove"),
            *packages
        )
    }

    fun purge(
        vararg packages: String,
        autoConfirm: Boolean = DEFAULT_AUTO_CONFIRM,
        autoRemove: Boolean = DEFAULT_AUTO_REMOVE
    ) {
        sendCommand(
            "purge",
            *If(autoConfirm).then("-y"),
            *If(autoRemove).then("--autoremove"),
            *packages
        )
    }

    fun clean() {
        sendCommand(
            "clean",
        )
    }
}