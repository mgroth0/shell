package matt.shell.commands.kill

import matt.lang.opt
import matt.log.warn.warn
import matt.shell.Shell
import matt.shell.proc.Pid
import matt.shell.proc.ProcessKillSignal

fun <R> Shell<R>.kill(
    pid: Pid,
    signal: ProcessKillSignal? = null,
    doNotThrowIfNoSuchProcess: Boolean = false
) = run {
    if (doNotThrowIfNoSuchProcess) {
        warn("doNotThrowIfNoSuchProcess is not implemented")
    }
    sendCommand(
        "kill",
        *opt(signal) { "-${name}" },
        pid.id
    )
}