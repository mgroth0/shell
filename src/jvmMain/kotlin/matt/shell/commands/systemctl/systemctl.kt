package matt.shell.commands.systemctl

import matt.shell.ControlledShellProgram
import matt.shell.Shell


val <R> Shell<R>.systemctl get() = SystemCtl(this)

class SystemCtl<R>(shell: Shell<R>) : ControlledShellProgram<R>(program = "systemctl", shell = shell) {
    fun status(service: String) = sendCommand("--no-pager", "status", service)
    fun start(service: String) = sendCommand("--no-pager", "start", service)
    fun enable(service: String) = sendCommand("--no-pager", "enable", service)
}


