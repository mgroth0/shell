package matt.shell.commands.pip

import matt.lang.common.If
import matt.shell.ControlledShellProgram
import matt.shell.common.Shell


/*unsafe because it is not called from a conda environment*/
val <R> Shell<R>.unsafePip get() = PipCommand(this)

class PipCommand<R>(shell: Shell<R>) : ControlledShellProgram<R>(
    shell = shell, program = "pip"
) {

    fun installPackages(
        quiet: Boolean = false,
        upgrade: Boolean = false,
        noDeps: Boolean = false,
        vararg packages: String
    ) = install(
        quiet = quiet,
        upgrade = upgrade,
        noDeps = noDeps,
        args = packages
    )


    fun installEditable(
        quiet: Boolean = false,
        noDeps: Boolean = false,
        pack: String
    ) = install(
        quiet = quiet,
        upgrade = false,
        noDeps = noDeps,
        args = arrayOf("-e", pack)
    )


    private fun install(
        quiet: Boolean = false,
        upgrade: Boolean = false,
        noDeps: Boolean = false,
        vararg args: String
    ) = sendCommand(
        "install",
        *If(quiet).then("-q"),
        *If(upgrade).then("--upgrade"),
        *If(noDeps).then("--no-deps"),
        *args
    )
}
