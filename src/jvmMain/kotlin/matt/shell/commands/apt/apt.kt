package matt.shell.commands.apt

import matt.lang.common.If
import matt.lang.function.Dsl
import matt.log.warn.common.warn
import matt.shell.ControlledShellProgram
import matt.shell.commands.apt.aptget.AptGet.Companion.DEFAULT_AUTO_CONFIRM
import matt.shell.commands.apt.aptget.AptGet.Companion.DEFAULT_AUTO_REMOVE
import matt.shell.commands.apt.aptget.AptGet.Companion.DEFAULT_FIX_MISSING
import matt.shell.commands.apt.options.AptOptionsBuilder
import matt.shell.common.Shell

interface LinuxPackageManager
interface AptLike : LinuxPackageManager

private const val WARNING = "Use apt-get. See: https://askubuntu.com/a/990838/557557"

@Suppress("DEPRECATION")
@Deprecated(WARNING)
val <R> Shell<R>.apt get() = Apt(this)

@Deprecated(WARNING)
class Apt<R>(shell: Shell<R>) : ControlledShellProgram<R>(program = "apt", shell = shell), AptLike {
    fun update(
        options: Dsl<AptOptionsBuilder> = {}
    ): R {
        val opts = AptOptionsBuilder().apply(options).current
        if (opts.lockTimeoutSeconds != null) {
            warn("this does not seem to work in any case")
        }
        return sendCommand(
            *opts.args,
            "update"
        )
    }

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
        autoConfirm: Boolean = DEFAULT_AUTO_CONFIRM
    ) = sendCommand(
        "purge",
        *If(autoRemove).then("--autoremove"),
        *If(autoConfirm).then("-y"),
        *packages
    )

    fun listInstalled() = sendCommand("list", "--installed")
}



