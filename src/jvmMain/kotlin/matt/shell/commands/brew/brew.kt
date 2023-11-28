package matt.shell.commands.brew

import matt.lang.If
import matt.shell.ControlledShellProgram
import matt.shell.Shell


val <R> Shell<R>.brew get() = BrewCommand(this)


class BrewCommand<R>(shell: Shell<R>) : ControlledShellProgram<R>(
    shell = shell,
    program = "brew"
) {
    fun install(
        formula: String,
        vararg options: String,
        cask: Boolean = false,
    ) = sendCommand(
        "install", *If(cask).then("--cask"), formula, *options
    )



    val services get() = BrewServicesCommand(this)
}


class BrewServicesCommand<R>(brewCommand: BrewCommand<R>) : ControlledShellProgram<R>(brewCommand, "services") {
    fun start(service: String) = sendCommand("start", service)
}


