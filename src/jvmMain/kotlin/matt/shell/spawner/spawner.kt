package matt.shell.spawner

import matt.async.thread.namedThread
import matt.file.model.file.types.AnyFolder
import matt.lang.function.SuspendOp
import matt.lang.shutdown.preaper.ProcessDestiny
import matt.model.code.output.OutputType
import matt.model.code.output.OutputType.STDERR
import matt.model.code.output.OutputType.STDOUT
import matt.prim.str.strings
import matt.shell.ConfigurableInPlaceShell
import matt.shell.commonj.context.ReapingShellExecutionContext
import matt.shell.proc.proc
import matt.shell.proc.signal.ProcessKillSignal.SIGKILL
import matt.shell.proc.signal.kill
import matt.shell.report.ShellErrException
import java.io.InputStream
import java.io.OutputStream
import kotlin.time.Duration

val ReapingShellExecutionContext.processSpawner get() = ExecProcessSpawner(this)

data class ExecProcessSpawner(
    override val executionContext: ReapingShellExecutionContext,
    private val throwOnErr: Boolean = false,
    val workingDir: AnyFolder? = null,
    val env: Map<String, String> = mapOf(),
    val timeout: Duration? = null
) : ConfigurableInPlaceShell<ProcessDestiny, ExecProcessSpawner> {

    override fun withInputStream(inputStream: InputStream?): ExecProcessSpawner {
        if (inputStream != null) {
            TODO("Not yet implemented")
        }
        return this
    }

    override fun sendCommand(vararg args: String): ProcessDestiny {
        val dest =
            with(executionContext) {
                proc(
                    wd = workingDir, env = env, args = (args.strings())
                )
            }
        val p = dest.process
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
        return dest
    }

    override fun doNotPrintCommand(op: ExecProcessSpawner.() -> Unit): ExecProcessSpawner = this

    override fun withWorkingDir(
        dir: AnyFolder,
        op: ExecProcessSpawner.() -> Unit
    ): ExecProcessSpawner = copy(workingDir = dir)

    override fun withUpdatedEnv(
        env: Map<String, String>,
        op: ExecProcessSpawner.() -> Unit
    ): ExecProcessSpawner = copy(env = this.env + env).apply(op)

    override fun withEnv(
        env: Map<String, String>,
        op: ExecProcessSpawner.() -> Unit
    ): ExecProcessSpawner = copy(env = env).apply(op)
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

/*avoid kotlinx.coroutines dependency*/
interface SuspendLaunchingScope {
    fun launch(op: SuspendOp)
}

fun Process.transferAllOutputToStdOutInJobs(
    scope: SuspendLaunchingScope,
    errTo: OutputType,
    buffered: Boolean = true
) {
    scope.launch {
        if (buffered) {
            errorStream.transferTo(
                when (errTo) {
                    STDERR -> System.err
                    STDOUT -> System.out
                }
            )
        } else {
            errorStream.transferToUnBuffered(
                when (errTo) {
                    STDERR -> System.err
                    STDOUT -> System.out
                }
            )
        }
    }
    scope.launch {
        if (buffered) {
            inputStream.transferTo(System.out)
        } else {
            inputStream.transferToUnBuffered(System.out)
        }
    }
}

/*based on build in transferTo*/
fun InputStream.transferToUnBuffered(out: OutputStream) {
    do {
        val read = this.read()
        if (read >= 0) {
            out.write(read)
            out.flush()
        }
    } while (read >= 0)
}
