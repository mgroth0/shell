
package matt.shell

import kotlinx.serialization.Serializable
import matt.async.thread.daemon
import matt.file.model.file.types.AnyFolder
import matt.lang.assertions.require.requireNot
import matt.lang.common.go
import matt.lang.model.file.AnyResolvableFilePath
import matt.lang.shutdown.preaper.ProcessReaper
import matt.lang.shutdown.preaper.use
import matt.log.j.DefaultLogger
import matt.log.j.SystemErrLogger
import matt.log.j.SystemOutLogger
import matt.model.op.prints.Prints
import matt.prim.str.strings
import matt.shell.PrintInSeq.CHARS
import matt.shell.PrintInSeq.LINES
import matt.shell.PrintInSeq.NO
import matt.shell.ShellVerbosity.Companion
import matt.shell.common.Commandable
import matt.shell.common.Shell
import matt.shell.common.ShellDSL
import matt.shell.common.context.ShellExecutionContext
import matt.shell.commonj.CommandReturner
import matt.shell.commonj.context.ReapingShellExecutionContext
import matt.shell.proc.awaitWhileSavingOutput
import matt.shell.proc.awaitWithoutSavingOutput
import matt.shell.proc.proc
import matt.shell.report.NonZeroShellResult
import matt.shell.report.ShellErrorReport
import matt.shell.report.ShellFullResult
import matt.shell.report.ShellResult
import java.io.InputStream

val Shell<*>.programPathContext
    get() = executionContext.shellProgramPathContext ?: error("programPathContext is required to be known")
val Shell<*>.inSingularityContainer
    get() = executionContext.inSingularityContainer ?: error("inSingularityContainer is required to be known")
val Shell<*>.inSlurmJob get() = executionContext.inSlurmJob ?: error("inSlurmJob is required to be known")
val Shell<*>.needsModules get() = executionContext.needsModules ?: error("needsModules is required to be known")


interface ConfigurableWorkingDir<T> {
    fun withWorkingDir(
        dir: AnyFolder,
        op: T.() -> Unit = {}
    ): T
}

interface DirectableShell<R, S : DirectableShell<R, S>> : Shell<R>, ConfigurableWorkingDir<S> {
    override fun withWorkingDir(
        dir: AnyFolder,
        op: S.() -> Unit
    ): S
}


@ShellDSL
interface ShellConfigurator<T : ShellConfigurator<T>> : ConfigurableWorkingDir<T> {
    fun withEnv(
        env: Map<String, String>,
        op: T.() -> Unit = {}
    ): T

    fun withUpdatedEnv(
        env: Map<String, String>,
        op: T.() -> Unit = {}
    ): T

    override fun withWorkingDir(
        dir: AnyFolder,
        op: T.() -> Unit
    ): T

    fun doNotPrintCommand(op: T.() -> Unit = {}): T
}
@ShellDSL
interface InPlaceShellConfigurator<T: InPlaceShellConfigurator<T>>: ShellConfigurator<T>  {
    fun withInputStream(inputStream: InputStream?): T
}


fun <T : ShellConfigurator<T>> ShellConfigurator<T>.withEnv(
    vararg pairs: Pair<String, String>,
    op: T.() -> Unit = {}
) = withEnv(
    pairs.toMap(), op
)

fun <T : ShellConfigurator<T>> ShellConfigurator<T>.withUpdatedEnv(
    vararg pairs: Pair<String, String>,
    op: T.() -> Unit = {}
) = withUpdatedEnv(
    pairs.toMap(), op
)


interface ConfigurableShell<R, T : ConfigurableShell<R, T>> : Shell<R>, ShellConfigurator<T>
interface ConfigurableInPlaceShell<R, T : ConfigurableInPlaceShell<R, T>> : Shell<R>, InPlaceShellConfigurator<T>, ConfigurableShell<R, T>


class ShellResultHandler<in S : ShellResult>(
    val nonZeroOkIf: (S) -> Boolean
)


enum class PrintInSeq { NO, LINES, CHARS }

@Serializable
data class ShellVerbosity(
    val printRunning: Boolean = false,
    val doNotPrintArgs: Boolean = false,
    val printInSequence: PrintInSeq = NO,
    val printRawOutput: Boolean = false,
    val explainOutput: Boolean = false,
    val verbose: Boolean = false,
    val outBeforeErr: Boolean = false
) {
    companion object {
        val SILENT = ShellVerbosity()
        val DEFAULT = SILENT
        val JUST_START = ShellVerbosity(printRunning = true)
        val START_AND_RAW_OUTPUT =
            ShellVerbosity(
                printRunning = true,
                printRawOutput = true
            )
        val START_AND_EXPLAIN_OUTPUT =
            ShellVerbosity(
                printRunning = true,
                explainOutput = true
            )
        val STREAM =
            ShellVerbosity(
                printRunning = true,
                printInSequence = LINES
            )
        val STREAM_CHARS =
            ShellVerbosity(
                printRunning = true,
                printInSequence = CHARS
            )
    }
}

fun interface ShellExecutor<out S: ShellResult> {
    context(ProcessReaper)
    fun execute(
        workingDir: AnyResolvableFilePath?,
        env: Map<String, String>,
        args: Array<out String>,
        inputStream: InputStream?
    ): S
}


fun interface ShellExecutorFactory<out S: ShellResult> {
    companion object {
        val DEFAULT_SAVING_OUTPUT =
            ShellExecutorFactory { verbosity, outLogger, errLogger ->
                DefaultOutputSavingShellExecutor(
                    verbosity = verbosity,
                    outLogger = outLogger,
                    errLogger = errLogger
                )
            }
        val DEFAULT_NOT_SAVING_OUTPUT =
            ShellExecutorFactory { verbosity, outLogger, errLogger ->
                DefaultOutputNotSavingShellExecutor(
                    verbosity = verbosity,
                    outLogger = outLogger,
                    errLogger = errLogger
                )
            }
    }

    fun executor(
        verbosity: ShellVerbosity,
        outLogger: Prints,
        errLogger: Prints
    ): ShellExecutor<S>
}


class DefaultOutputSavingShellExecutor(
    verbosity: ShellVerbosity,
    outLogger: Prints,
    errLogger: Prints
): DefaultShellExecutor<ShellFullResult>(verbosity = verbosity, outLogger = outLogger, errLogger = errLogger)  {
    override fun Process.awaitTheRightWay(): ShellFullResult = awaitWhileSavingOutput(verbosity = verbosity, outLogger = outLogger, errLogger = errLogger)
}
class DefaultOutputNotSavingShellExecutor(
    verbosity: ShellVerbosity,
    outLogger: Prints,
    errLogger: Prints
): DefaultShellExecutor<ShellResult>(verbosity = verbosity, outLogger = outLogger, errLogger = errLogger)  {
    override fun Process.awaitTheRightWay(): ShellResult = awaitWithoutSavingOutput(verbosity = verbosity, outLogger = outLogger, errLogger = errLogger)
}

abstract class DefaultShellExecutor<S: ShellResult>(
    protected val verbosity: ShellVerbosity,
    protected val outLogger: Prints,
    protected val errLogger: Prints
) : ShellExecutor<S> {

    protected abstract fun Process.awaitTheRightWay(): S

    context(ProcessReaper)
    final override fun execute(
        workingDir: AnyResolvableFilePath?,
        env: Map<String, String>,
        args: Array<out String>,
        inputStream: InputStream?
    ): S {
        val dest =
            proc(
                wd = workingDir,
                args = args,
                env = env,
                destroyRecursiveSubprocesses = true
            )
        val p = dest.process

        var interrupting = false
        val writerThread =
            if (inputStream != null) {
                daemon("input stream transfer") {
                    try {
                        val out = p.outputStream
                        while (true) {
                            val b = inputStream.read()
                            if (b < 0) break
                            out.write(b)
                            out.flush()
                        }
                    } catch (e: InterruptedException) {
                        if (!interrupting) throw e
                    }
                }
            } else null
        try {
            return dest.use {
                it.awaitTheRightWay()
            }
        } finally {
            interrupting = true
            writerThread?.interrupt()
        }
    }
}

val ReapingShellExecutionContext.execReturners
    get() = ExecReturners(this)

class ExecReturners(
    executionContext: ReapingShellExecutionContext
) {
    val silent by lazy {
        ExecReturner(
            executionContext = executionContext,
            verbosity = ShellVerbosity.SILENT
        )
    }
    val stream by lazy {
        ExecReturner(
            executionContext = executionContext,
            verbosity = ShellVerbosity.STREAM
        )
    }
    val streamChars by lazy {
        ExecReturner(
            executionContext = executionContext,
            verbosity = ShellVerbosity.STREAM_CHARS
        )
    }
}

data class ExecReturner(
    override val executionContext: ReapingShellExecutionContext,
    private val verbosity: ShellVerbosity,
    private val workingDir: AnyFolder? = null,
    private val env: Map<String, String> = mapOf(),
    private val outLogger: Prints = SystemOutLogger.apply { includeTimeInfo = false },
    private val errLogger: Prints = SystemErrLogger.apply { includeTimeInfo = false },
    private val metaLogger: Prints = outLogger,
    private val resultHandler: ShellResultHandler<ShellFullResult>? = null,
    private val inputStream: InputStream? = null
) : DirectableShell<String, ExecReturner>, ConfigurableInPlaceShell<String, ExecReturner> {

    override fun withInputStream(inputStream: InputStream?): ExecReturner = copy(inputStream = inputStream)

    override fun withEnv(
        env: Map<String, String>,
        op: ExecReturner.() -> Unit
    ) = copy(env = env).apply(op)

    override fun withUpdatedEnv(
        env: Map<String, String>,
        op: ExecReturner.() -> Unit
    ) =
        copy(env = this@ExecReturner.env + env).apply(op)

    override fun withWorkingDir(
        dir: AnyFolder,
        op: ExecReturner.() -> Unit
    ) = copy(workingDir = dir).apply(op)

    fun <R> withVerbosity(
        verbosity: ShellVerbosity,
        op: ExecReturner.() -> R
    ) = copy(verbosity = verbosity).run(op)

    override fun doNotPrintCommand(op: ExecReturner.() -> Unit) =
        copy(verbosity = verbosity.copy(doNotPrintArgs = true)).apply(op)


    override fun sendCommand(vararg args: String): String {
        with(executionContext) {
            return shell(
                *(args.strings()),
                verbosity = verbosity,
                outLogger = outLogger,
                errLogger = errLogger,
                metaLogger = metaLogger,
                resultHandler = resultHandler,
                workingDir = workingDir,
                env = env,
                inputStream = inputStream
            )
        }
    }
}


interface UnControlledCommand<R>
interface ShellProgram<R> : UnControlledCommand<R>, Commandable<R> {
    override fun sendCommand(vararg args: String): R
}


class SimpleShellProgram<R>(
    private val shell: Commandable<R>,
    private val program: String,
    vararg val programArgs: String
) : ShellProgram<R> {
    override fun sendCommand(vararg args: String): R = shell.sendCommand(program, *programArgs, *args)

    fun withAdditionalProgramArgs(vararg additionalProgramArgs: String) =
        SimpleShellProgram(shell, program = program, programArgs = arrayOf(*programArgs, *additionalProgramArgs))
}

class SimpleShellToolbox<R>(
    val shell: Commandable<R>
) : ShellProgram<R> {
    override fun sendCommand(vararg args: String): R = shell.sendCommand(*args)
}

abstract class AbstractControlledShellProgram<R> {
    protected abstract fun sendCommand(vararg args: String): R
}

abstract class ControlledShellProgram<R>(
    private val program: ShellProgram<R>,
    private vararg val programArgs: String
): AbstractControlledShellProgram<R>() {
    constructor(
        program: String,
        vararg programArgs: String,
        shell: Commandable<R>
    ) : this(SimpleShellProgram(shell = shell, program = program, programArgs = programArgs))


    constructor(
        program: ControlledShellProgram<R>,
        vararg programArgs: String
    ) : this(program.program, *program.programArgs, *programArgs)

    fun withAdditionalProgramArgs() = program

    final override fun sendCommand(vararg args: String): R = program.sendCommand(*programArgs, *args)
}

abstract class ControlledShellToolbox<R>(private val program: ShellProgram<R>) {
    constructor(
        shell: Commandable<R>
    ) : this(SimpleShellToolbox(shell = shell))

    protected fun sendCommand(vararg args: String): R = program.sendCommand(*args)
}

context (ProcessReaper)
fun exec(
    wd: AnyResolvableFilePath?,
    vararg args: String
) = proc(
    wd,
    *args
).process.waitFor() == 0

context(ReapingShellExecutionContext)
fun <R> shells(
    verbosity: ShellVerbosity = ShellVerbosity.SILENT,
    workingDir: AnyFolder? = null,
    env: Map<String, String> = mapOf(),
    op: ExecReturner.() -> R
) = ExecReturner(
    executionContext = this@ReapingShellExecutionContext,
    verbosity = verbosity,
    workingDir = workingDir,
    env = env
).run(op)

context(ReapingShellExecutionContext)
val shell: Shell<String>
    get() = execReturners.silent

context(ProcessReaper)
fun shell(
    vararg args: String,
    workingDir: AnyFolder? = null,
    env: Map<String, String> = mapOf(),
    verbosity: ShellVerbosity = Companion.DEFAULT,
    outLogger: Prints = DefaultLogger,
    errLogger: Prints = DefaultLogger,
    metaLogger: Prints = outLogger,
    executorFactory: ShellExecutorFactory<ShellFullResult> = ShellExecutorFactory.DEFAULT_SAVING_OUTPUT,
    resultHandler: ShellResultHandler<ShellFullResult>? = null,
    inputStream: InputStream? = null
) = FullResultShellRunner(
    args = args,
    workingDir = workingDir,
    env = env,
    verbosity = verbosity,
    outLogger = outLogger,
    errLogger = errLogger,
    metaLogger = metaLogger,
    resultHandler = resultHandler,
    executorFactory = executorFactory,
    inputStream = inputStream
).run().output

context(ReapingShellExecutionContext)
fun streamingMemorySafeShells(
    verbosity: ShellVerbosity = Companion.SILENT,
    workingDir: AnyFolder? = null,
    env: Map<String, String> = mapOf(),
    op: ExecStreamer.() -> Unit
) {
    ExecStreamer(
        executionContext = this@ReapingShellExecutionContext,
        verbosity = verbosity,
        workingDir = workingDir,
        env = env,
        processReaper = this@ReapingShellExecutionContext
    ).apply(op)
}


context(ProcessReaper)
fun streamingMemorySafeShell(
    vararg args: String,
    workingDir: AnyFolder? = null,
    env: Map<String, String> = mapOf(),
    verbosity: ShellVerbosity = Companion.DEFAULT,
    logger: Prints = DefaultLogger,
    resultHandler: ShellResultHandler<ShellResult>? = null,
    inputStream: InputStream? = null
) = MemSafeShellRunner(
    args = args,
    workingDir = workingDir,
    env = env,
    verbosity = verbosity,
    logger = logger,
    resultHandler = resultHandler,
    inputStream = inputStream
).run()


abstract class ShellRunner<S: ShellResult>(
    private vararg val args: String,
    private val workingDir: AnyFolder? = null,
    private val env: Map<String, String> = mapOf(),
    private val verbosity: ShellVerbosity = Companion.DEFAULT,
    private val outLogger: Prints = DefaultLogger,
    private val errLogger: Prints = DefaultLogger,
    private val metaLogger: Prints = outLogger,
    private val executorFactory: ShellExecutorFactory<S>,
    private val resultHandler: ShellResultHandler<S>? = null,
    private val inputStream: InputStream?
) {
    context(ProcessReaper)
    fun run(): S {
        if (verbosity.printRunning) {
            if (verbosity.doNotPrintArgs) metaLogger.println("running command (hidden args)")
            else metaLogger.println("running command(${args.size}): ${args.joinToString(" ")}")
        }
        val result =
            executorFactory.executor(
                verbosity = verbosity,
                outLogger = outLogger,
                errLogger = errLogger
            ).execute(
                workingDir = workingDir,
                env = env,
                args = args,
                inputStream = inputStream
            )
        if (verbosity.explainOutput) metaLogger.println("output: ${(result as? ShellFullResult)?.output}")

        if (result.code != 0) {
            resultHandler?.nonZeroOkIf?.go { isOk ->

                if (isOk(result)) return result
            }

            val report =
                ShellErrorReport(
                    workingDir = workingDir,
                    env = env,
                    args = args,
                    result = result
                )

            throw NonZeroShellResult(
                result,
                report
            )
        }

        return result
    }
}

class MemSafeShellRunner(
    vararg args: String,
    workingDir: AnyFolder? = null,
    env: Map<String, String> = mapOf(),
    verbosity: ShellVerbosity = Companion.DEFAULT,
    logger: Prints = DefaultLogger,
    resultHandler: ShellResultHandler<ShellResult>? = null,
    inputStream: InputStream?
) : ShellRunner<ShellResult>(
        args = args,
        workingDir = workingDir,
        env = env,
        verbosity = verbosity,
        outLogger = logger,
        errLogger = logger,
        resultHandler = resultHandler,
        inputStream = inputStream,
        executorFactory = ShellExecutorFactory.DEFAULT_NOT_SAVING_OUTPUT
    ) {
    init {
        requireNot(verbosity.explainOutput)
    }
}

class FullResultShellRunner(
    vararg args: String,
    workingDir: AnyFolder? = null,
    env: Map<String, String> = mapOf(),
    verbosity: ShellVerbosity = Companion.DEFAULT,
    errLogger: Prints = DefaultLogger,
    outLogger: Prints = DefaultLogger,
    metaLogger: Prints = outLogger,
    executorFactory: ShellExecutorFactory<ShellFullResult> = ShellExecutorFactory.DEFAULT_SAVING_OUTPUT,
    resultHandler: ShellResultHandler<ShellFullResult>? = null,
    inputStream: InputStream?
) : ShellRunner<ShellFullResult>(
        args = args,
        workingDir = workingDir,
        env = env,
        verbosity = verbosity,
        errLogger = errLogger,
        outLogger = outLogger,
        metaLogger = metaLogger,
        resultHandler = resultHandler,
        executorFactory = executorFactory,
        inputStream = inputStream
    )

context (ProcessReaper)
val ShellExecutionContext.execStreamers
    get() = ExecStreamers(this, this@ProcessReaper)

class ExecStreamers(
    executionContext: ShellExecutionContext,
    processReaper: ProcessReaper
) {
    val silent by lazy {
        ExecStreamer(
            executionContext = executionContext,
            verbosity = ShellVerbosity.SILENT,
            processReaper = processReaper
        )
    }
    val stream by lazy {
        ExecStreamer(
            executionContext = executionContext,
            verbosity = ShellVerbosity.STREAM,
            processReaper = processReaper
        )
    }
    val streamChars by lazy {
        ExecStreamer(
            executionContext = executionContext,
            verbosity = ShellVerbosity.STREAM_CHARS,
            processReaper = processReaper
        )
    }
}

data class ExecStreamer(
    override val executionContext: ShellExecutionContext,
    private val verbosity: ShellVerbosity,
    private val workingDir: AnyFolder? = null,
    private val env: Map<String, String> = mapOf(),
    private val logger: Prints = SystemOutLogger.apply { includeTimeInfo = false },
    private val resultHandler: ShellResultHandler<ShellResult>? = null,
    private val processReaper: ProcessReaper

) : DirectableShell<ShellResult, ExecStreamer>, ConfigurableInPlaceShell<ShellResult, ExecStreamer> {

    override fun withInputStream(inputStream: InputStream?): ExecStreamer {
        if (inputStream != null) {
            TODO("Not yet implemented")
        }
        return this
    }

    override fun withEnv(
        env: Map<String, String>,
        op: ExecStreamer.() -> Unit
    ) = copy(env = env).apply(op)

    override fun withUpdatedEnv(
        env: Map<String, String>,
        op: ExecStreamer.() -> Unit
    ) =
        copy(env = this@ExecStreamer.env + env).apply(op)

    override fun withWorkingDir(
        dir: AnyFolder,
        op: ExecStreamer.() -> Unit
    ) = copy(workingDir = dir).apply(op)

    fun <R> withVerbosity(
        verbosity: ShellVerbosity,
        op: ExecStreamer.() -> R
    ) = copy(verbosity = verbosity).run(op)

    override fun doNotPrintCommand(op: ExecStreamer.() -> Unit) =
        copy(verbosity = verbosity.copy(doNotPrintArgs = true)).apply(op)

    override fun sendCommand(vararg args: String): ShellResult {
        with(processReaper) {
            return streamingMemorySafeShell(
                *(args.strings()),
                verbosity = verbosity,
                logger = logger,
                resultHandler = resultHandler,
                workingDir = workingDir,
                env = env
            )
        }
    }
}

val Shell<*>.command get() = executionContext.command
val ShellExecutionContext.command get() = CommandReturner(this)


