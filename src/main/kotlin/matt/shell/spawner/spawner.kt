package matt.shell.spawner

import matt.model.data.file.IDFile
import matt.prim.str.strings
import matt.shell.ConfigurableShell
import matt.shell.context.ShellExecutionContext
import matt.shell.proc.ProcessKillSignal.SIGKILL
import matt.shell.proc.kill
import matt.shell.proc.proc
import matt.shell.report.ShellErrException
import matt.shell.spawner.OutputType.STDERR
import matt.shell.spawner.OutputType.STDOUT
import kotlin.concurrent.thread
import kotlin.time.Duration

data class ExecProcessSpawner(
    override val executionContext: ShellExecutionContext,
    private val throwOnErr: Boolean = false,
    val workingDir: IDFile? = null,
    val env: Map<String, String> = mapOf(),
    val timeout: Duration? = null
) : ConfigurableShell<Process, ExecProcessSpawner> {
    override fun sendCommand(vararg args: String): Process {
        val p = proc(
            wd = workingDir, env = env, args = (args.strings())
        )
        if (timeout != null) {
            thread(name = "timeoutExecutor") {
                Thread.sleep(timeout.inWholeMilliseconds)
                if (p.isAlive) {
                    println("killing process $p after timeout of $timeout...")
                    with(executionContext) {
                        p.kill(SIGKILL)
                    }
                    println("killed")
                } else {
                    println("$p is already dead")
                }
            }
        }
        if (throwOnErr) {
            thread(isDaemon = true, name = "process thread") {
                p.errorReader().lines().forEach {
                    throw ShellErrException(it)
                }
            }
        }
        return p
    }

    override fun doNotPrintCommand(op: ExecProcessSpawner.() -> Unit): ExecProcessSpawner {
        return this
    }

    override fun withWorkingDir(
        dir: IDFile,
        op: ExecProcessSpawner.() -> Unit
    ): ExecProcessSpawner {
        return copy(workingDir = dir)
    }

    override fun withUpdatedEnv(
        env: Map<String, String>,
        op: ExecProcessSpawner.() -> Unit
    ): ExecProcessSpawner {
        return copy(env = this.env + env).apply(op)
    }

    override fun withEnv(
        env: Map<String, String>,
        op: ExecProcessSpawner.() -> Unit
    ): ExecProcessSpawner {
        return copy(env = env).apply(op)
    }
}


enum class OutputType {
    STDOUT, STDERR
}

fun Process.transferAllOutputToStdOutInThreads(
    errTo: OutputType
) {
    thread(isDaemon = true) {
        errorStream.transferTo(
            when (errTo) {
                STDERR -> System.err
                STDOUT -> System.out
            }
        )
    }
    thread(isDaemon = true) {
        inputStream.transferTo(System.out)
    }
}