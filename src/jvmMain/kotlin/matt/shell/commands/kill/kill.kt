package matt.shell.commands.kill

import matt.lang.common.opt
import matt.log.warn.common.warn
import matt.shell.common.Shell
import matt.shell.proc.pid.Pid
import matt.shell.proc.signal.ProcessKillSignal

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
        *opt(signal) { "-$name" },
        pid.id.toString()
    )
}
