package matt.shell.proc.pid

import matt.log.warn.warn
import matt.prim.str.lower
import matt.shell.commands.kill.kill
import matt.shell.context.ReapingShellExecutionContext
import matt.shell.execReturners
import matt.shell.proc.signal.ProcessKillSignal
import matt.shell.proc.signal.ProcessKillSignal.SIGINT
import matt.shell.proc.signal.ProcessKillSignal.SIGKILL
import matt.shell.proc.signal.ProcessKillSignal.SIGTERM
import matt.shell.report.NonZeroShellResult

fun Process.myPid() = Pid(pid())

@JvmInline
value class Pid(val id: Long) {

    context(ReapingShellExecutionContext)
    fun kill(
        signal: ProcessKillSignal = SIGKILL,
        doNotThrowIfNoSuchProcess: Boolean = false
    ) {
        fun op() = execReturners.silent.kill(this, signal)
        if (doNotThrowIfNoSuchProcess) {
            try {
                op()
            } catch (e: NonZeroShellResult) {
                if (
                    e.code == 47202 ||
                    (e.code == 1 && e.output?.lower()
                        ?.contains("no such process") == true)  /*error code must be different for different shells in this situation. Seems like it might be different between bash and zsh. I think 1 might be zsh and 47202 might be bash, but not sure. Another possibility is that 47202 was just a process ID and I mistook it for an error code once.*/
                ) {
                    warn("\"no such process\" when trying to kill $id")
                } else {
                    throw e
                }
            }
        } else {
            op()
        }

    }

    context(ReapingShellExecutionContext)
    fun terminate() {
        kill(SIGTERM)
    }

    context(ReapingShellExecutionContext)
    fun interrupt() {
        kill(SIGINT)
    }

    context(ReapingShellExecutionContext)
    fun kill() {
        kill(SIGKILL)
    }


}
