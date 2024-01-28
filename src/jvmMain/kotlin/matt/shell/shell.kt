@file:JvmName("ShellJvmKt")

package matt.shell

import kotlinx.serialization.Serializable
import matt.async.thread.daemon
import matt.lang.anno.Open
import matt.lang.assertions.require.requireNot
import matt.lang.go
import matt.lang.model.file.AnyResolvableFilePath
import matt.lang.model.file.types.AnyFolder
import matt.lang.shutdown.preaper.ProcessReaper
import matt.log.DefaultLogger
import matt.log.SystemErrLogger
import matt.log.SystemOutLogger
import matt.log.warn.dumpStack
import matt.log.warn.warn
import matt.model.op.prints.Prints
import matt.prim.str.joinWithSpaces
import matt.prim.str.strings
import matt.service.MattService
import matt.shell.PrintInSeq.CHARS
import matt.shell.PrintInSeq.LINES
import matt.shell.PrintInSeq.NO
import matt.shell.ShellVerbosity.Companion
import matt.shell.context.ReapingShellExecutionContext
import matt.shell.context.ShellExecutionContext
import matt.shell.proc.await
import matt.shell.proc.proc
import matt.shell.proc.use
import matt.shell.report.NonZeroShellResult
import matt.shell.report.ShellErrorReport
import matt.shell.report.ShellFullResult
import matt.shell.report.ShellResult
import java.io.InputStream

@DslMarker
annotation class ShellDSL

@ShellDSL
interface Commandable<R> {
    fun sendCommand(vararg args: String): R

    @Open
    fun sendCommand(command: Command): R {
        return sendCommand(*command.commands.toTypedArray())
    }
}

@ShellDSL
interface Shell<R : Any?> : MattService, Commandable<R> {
    @Open
    val AnyResolvableFilePath.pathOp: String get() = path
    val executionContext: ShellExecutionContext
}

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


class ShellResultHandler<S : ShellResult>(
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
        val START_AND_RAW_OUTPUT = ShellVerbosity(
            printRunning = true,
            printRawOutput = true
        )
        val START_AND_EXPLAIN_OUTPUT = ShellVerbosity(
            printRunning = true,
            explainOutput = true
        )
        val STREAM = ShellVerbosity(
            printRunning = true,
            printInSequence = LINES
        )
        val STREAM_CHARS = ShellVerbosity(
            printRunning = true,
            printInSequence = CHARS
        )
    }
}

fun interface ShellExecutor {
    context(ProcessReaper)
    fun execute(
        workingDir: AnyResolvableFilePath?,
        env: Map<String, String>,
        args: Array<out String>,
        inputStream: InputStream?
    ): ShellResult
}


fun interface ShellExecutorFactory {
    companion object {
        val DEFAULT = ShellExecutorFactory { saveOutput, verbosity, outLogger, errLogger ->
            DefaultShellExecutor(
                saveOutput = saveOutput,
                verbosity = verbosity,
                outLogger = outLogger,
                errLogger = errLogger,
            )
        }
    }

    fun executor(
        saveOutput: Boolean,
        verbosity: ShellVerbosity,
        outLogger: Prints,
        errLogger: Prints,
    ): ShellExecutor
}


class DefaultShellExecutor(
    private val saveOutput: Boolean,
    private val verbosity: ShellVerbosity,
    private val outLogger: Prints,
    private val errLogger: Prints,
) : ShellExecutor {

    context(ProcessReaper)
    override fun execute(
        workingDir: AnyResolvableFilePath?,
        env: Map<String, String>,
        args: Array<out String>,
        inputStream: InputStream?
    ): ShellResult {
        val p = proc(
            wd = workingDir,
            args = args,
            env = env
        )

        var interrupting = false
        val writerThread = if (inputStream != null) {
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
            return p.use {
                p.await(
                    verbosity = verbosity,
                    outLogger = outLogger,
                    errLogger = errLogger,
                    saveOutput = saveOutput
                )
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
    executionContext: ReapingShellExecutionContext,
) {
    val silent by lazy {
        ExecReturner(
            executionContext = executionContext,
            verbosity = ShellVerbosity.SILENT,
        )
    }
    val stream by lazy {
        ExecReturner(
            executionContext = executionContext,
            verbosity = ShellVerbosity.STREAM,
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
) : DirectableShell<String, ExecReturner>, ConfigurableShell<String, ExecReturner> {

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
    override fun sendCommand(vararg args: String): R {
        return shell.sendCommand(program, *programArgs, *args)
    }

    fun withAdditionalProgramArgs(vararg additionalProgramArgs: String) =
        SimpleShellProgram(shell, program = program, programArgs = arrayOf(*programArgs, *additionalProgramArgs))
}

class SimpleShellToolbox<R>(
    val shell: Commandable<R>,
) : ShellProgram<R> {
    override fun sendCommand(vararg args: String): R {
        return shell.sendCommand(*args)
    }
}

abstract class ControlledShellProgram<R>(
    private val program: ShellProgram<R>,
    private vararg val programArgs: String
) {
    constructor(
        program: String,
        vararg programArgs: String,
        shell: Commandable<R>
    ) : this(SimpleShellProgram(shell = shell, program = program, programArgs = programArgs))


    constructor(
        program: ControlledShellProgram<R>,
        vararg programArgs: String,
    ) : this(program.program, *program.programArgs, *programArgs)

    fun withAdditionalProgramArgs() = program

    protected fun sendCommand(vararg args: String): R {
        return program.sendCommand(*programArgs, *args)
    }
}

abstract class ControlledShellToolbox<R>(private val program: ShellProgram<R>) {
    constructor(
        shell: Commandable<R>
    ) : this(SimpleShellToolbox(shell = shell))

    protected fun sendCommand(vararg args: String): R {
        return program.sendCommand(*args)
    }
}

context (ProcessReaper)
fun exec(
    wd: AnyResolvableFilePath?,
    vararg args: String
) = proc(
    wd,
    *args
).waitFor() == 0

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
    env = env,
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
    executorFactory: ShellExecutorFactory = ShellExecutorFactory.DEFAULT,
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


abstract class ShellRunner<S : ShellResult>(
    private vararg val args: String,
    private val workingDir: AnyFolder? = null,
    private val env: Map<String, String> = mapOf(),
    private val verbosity: ShellVerbosity = Companion.DEFAULT,
    private val outLogger: Prints = DefaultLogger,
    private val errLogger: Prints = DefaultLogger,
    private val metaLogger: Prints = outLogger,
    private val executorFactory: ShellExecutorFactory = ShellExecutorFactory.DEFAULT,
    private val resultHandler: ShellResultHandler<S>? = null,
    private val inputStream: InputStream?
) {
    protected abstract val saveOutput: Boolean
    context(ProcessReaper)
    fun run(): S {
        if (verbosity.printRunning) {
            if (verbosity.doNotPrintArgs) metaLogger.println("running command (hidden args)")
            else metaLogger.println("running command(${args.size}): ${args.joinToString(" ")}")
        }
        val result = executorFactory.executor(
            saveOutput = saveOutput,
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
                @Suppress("UNCHECKED_CAST")
                if (isOk(result as S)) return result
            }

            val report = ShellErrorReport(
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
        @Suppress("UNCHECKED_CAST")
        return result as S
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
    inputStream = inputStream
) {
    init {
        requireNot(verbosity.explainOutput)
    }

    override val saveOutput = false
}

class FullResultShellRunner(
    vararg args: String,
    workingDir: AnyFolder? = null,
    env: Map<String, String> = mapOf(),
    verbosity: ShellVerbosity = Companion.DEFAULT,
    errLogger: Prints = DefaultLogger,
    outLogger: Prints = DefaultLogger,
    metaLogger: Prints = outLogger,
    executorFactory: ShellExecutorFactory = ShellExecutorFactory.DEFAULT,
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
) {
    override val saveOutput = true
}

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
) : DirectableShell<ShellResult, ExecStreamer>, ConfigurableShell<ShellResult, ExecStreamer> {

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
                env = env,
            )
        }
    }
}

val Shell<*>.command get() = executionContext.command
val ShellExecutionContext.command get() = CommandReturner(this)

class CommandReturner(
    override val executionContext: ShellExecutionContext
) : Shell<Command> {
    override fun sendCommand(vararg args: String) = Command(args.map { it.toString() })
}


@Serializable
/*value class here would be ideal but this failed with https://youtrack.jetbrains.com/issue/KT-57647/Serialization-IllegalAccessError-Update-to-static-final-field-caused-by-serializable-value-class?s=update-to-static-final-field-attempted-from-a-different-method-constructor-impl-than-the-initializer-method-clinit-when*/
/*@JvmInline*/
data /*value*/ class Command(val commands: List<String>) {

    fun rawWithNoEscaping() = commands.joinWithSpaces()

    override fun toString(): String {
        warn("don't use vague toString()")
        dumpStack()
        return rawWithNoEscaping()
    }

    fun asArray() = commands.toTypedArray()

    infix fun pipedTo(consumer: Command) = Command(commands + "|" + consumer.commands)

    infix fun pipedToFile(file: AnyResolvableFilePath) = Command(commands + ">" + file.path)

    infix fun and(consumer: Command) = Command(commands + "&&" + consumer.commands)

}