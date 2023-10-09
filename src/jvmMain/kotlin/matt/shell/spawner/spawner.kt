package matt.shell.spawner

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import matt.async.thread.namedThread
import matt.lang.model.file.types.Folder
import matt.model.code.output.OutputType
import matt.model.code.output.OutputType.STDERR
import matt.model.code.output.OutputType.STDOUT
import matt.prim.str.strings
import matt.shell.ConfigurableShell
import matt.shell.context.ReapingShellExecutionContext
import matt.shell.proc.ProcessKillSignal.SIGKILL
import matt.shell.proc.kill
import matt.shell.proc.proc
import matt.shell.report.ShellErrException
import kotlin.time.Duration

data class ExecProcessSpawner(
    override val executionContext: ReapingShellExecutionContext,
    private val throwOnErr: Boolean = false,
    val workingDir: Folder? = null,
    val env: Map<String, String> = mapOf(),
    val timeout: Duration? = null
) : ConfigurableShell<Process, ExecProcessSpawner> {

    override fun sendCommand(vararg args: String): Process {
        val p = with(executionContext) {
            proc(
                wd = workingDir, env = env, args = (args.strings())
            )
        }
        if (timeout != null) {
            namedThread(name = "timeoutExecutor") {
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
            namedThread(isDaemon = true, name = "process thread") {
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
        dir: Folder,
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


fun Process.transferAllOutputToStdOutInThreads(
    errTo: OutputType
) {
    namedThread(isDaemon = true, name = "transferAllOutputToStdOutInThreads Thread 1") {
        errorStream.transferTo(
            when (errTo) {
                STDERR -> System.err
                STDOUT -> System.out
            }
        )
    }
    namedThread(isDaemon = true, name = "transferAllOutputToStdOutInThreads Thread 2") {
        inputStream.transferTo(System.out)
    }
}

fun Process.transferAllOutputToStdOutInJobs(
    scope: CoroutineScope,
    errTo: OutputType
) {
    scope.launch {
        errorStream.transferTo(
            when (errTo) {
                STDERR -> System.err
                STDOUT -> System.out
            }
        )
    }
    scope.launch {
        inputStream.transferTo(System.out)
    }
}