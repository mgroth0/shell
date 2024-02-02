package matt.shell.proc.signal

import matt.shell.context.ReapingShellExecutionContext
import matt.shell.proc.pid.myPid
import matt.shell.proc.signal.ProcessKillSignal.SIGINT
import matt.shell.proc.signal.ProcessKillSignal.SIGKILL


enum class ProcessKillSignal {
    SIGTERM, SIGINT, SIGKILL
}


context (ReapingShellExecutionContext)
fun Process.kill(signal: ProcessKillSignal = SIGKILL) = myPid().kill(signal)

context (ReapingShellExecutionContext)
fun Process.interrupt() = kill(SIGINT)
