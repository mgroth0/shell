package matt.shell.commands.apt.aptget

import matt.lang.If
import matt.lang.function.DSL
import matt.log.warn.warn
import matt.shell.ControlledShellProgram
import matt.shell.Shell
import matt.shell.commands.apt.AptLike
import matt.shell.commands.apt.options.AptOptionsBuilder

val <R> Shell<R>.aptGet get() = AptGet(this)

class AptGet<R>(shell: Shell<R>) : ControlledShellProgram<R>(shell = shell, program = "apt-get"), AptLike {

    companion object {
        const val DEFAULT_AUTO_CONFIRM = false
        const val DEFAULT_FIX_MISSING = false
        internal const val DEFAULT_AUTO_REMOVE = false
    }

    fun update(
        options: DSL<AptOptionsBuilder> = {}
    ): R {
        val opts = AptOptionsBuilder().apply(options).current
        if (opts.lockTimeoutSeconds != null) {
            warn("according to a comment here: https://unix.stackexchange.com/a/277255/175318 the lock timeout feature only exists in apt, not apt-get")
        }
        return sendCommand(
            *opts.args,
            "update"
        )
    }

    fun install(
        vararg packages: String,
        autoConfirm: Boolean = DEFAULT_AUTO_CONFIRM,
        reinstall: Boolean = false,
        options: DSL<AptOptionsBuilder> = {}
    ): R {
        val opts = AptOptionsBuilder().apply(options).current
        if (opts.lockTimeoutSeconds != null) {
            warn("according to a comment here: https://unix.stackexchange.com/a/277255/175318 the lock timeout feature only exists in apt, not apt-get")
        }
        return sendCommand(
            "install",
            *If(autoConfirm).then("-y"),
            *If(reinstall).then("--reinstall"),
            *opts.args,
            * packages
        )
    }

    fun remove(
        vararg packages: String,
        autoConfirm: Boolean = DEFAULT_AUTO_CONFIRM,
        autoRemove: Boolean = DEFAULT_AUTO_REMOVE
    ): R {
        warn("Are you sure you do not want to use purge?")
        return sendCommand(
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
    ): R {
        return sendCommand(
            "purge",
            *If(autoConfirm).then("-y"),
            *If(autoRemove).then("--autoremove"),
            *packages
        )
    }

    fun clean(): R {
        return sendCommand(
            "clean",
        )
    }
}

