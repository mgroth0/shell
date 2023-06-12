package matt.shell

import kotlinx.serialization.Serializable
import matt.lang.go
import matt.log.DefaultLogger
import matt.log.SystemErrLogger
import matt.log.SystemOutLogger
import matt.log.warn.warn
import matt.model.data.file.FilePath
import matt.model.data.file.IDFile
import matt.model.data.file.IDFolder
import matt.model.op.prints.Prints
import matt.prim.str.joinWithSpaces
import matt.prim.str.strings
import matt.service.MattService
import matt.shell.PrintInSeq.CHARS
import matt.shell.PrintInSeq.LINES
import matt.shell.PrintInSeq.NO
import matt.shell.ShellProgramPathContext.HomeBrew
import matt.shell.ShellProgramPathContext.InPath
import matt.shell.ShellVerbosity.Companion
import matt.shell.proc.await
import matt.shell.proc.proc
import matt.shell.report.NonZeroShellResult
import matt.shell.report.ShellErrorReport
import matt.shell.report.ShellFullResult
import matt.shell.report.ShellResult

@DslMarker
annotation class ShellDSL

@ShellDSL
interface Commandable<R> {
    fun sendCommand(vararg args: String): R
    fun sendCommand(command: Command): R {
        return sendCommand(*command.commands.toTypedArray())
    }
}

@ShellDSL
interface Shell<R : Any?> : MattService, Commandable<R> {
    val FilePath.pathOp: String get() = filePath
    val programPathContext: ShellProgramPathContext
}

enum class ShellProgramPathContext {
    InPath, HomeBrew
}

val DEFAULT_MAC_PROGRAM_PATH_CONTEXT = HomeBrew
val DEFAULT_LINUX_PROGRAM_PATH_CONTEXT = InPath
val DEFAULT_WINDOWS_PROGRAM_PATH_CONTEXT = InPath

//interface ShellDomain<R> : Shell<R> {
//    val engine: Shell<R>
//    override fun sendCommand(vararg args: Any): R {
//        return engine.sendCommand(args)
//    }
//}
//
//data class NonSpecificShellDomain<R>(override val engine: Shell<R>) : ShellDomain<R>


interface ConfigurableWorkingDir<T> {
    fun withWorkingDir(
        dir: IDFile,
        op: T.() -> Unit = {}
    ): T
}

interface DirectableShell<R, S : DirectableShell<R, S>> : Shell<R>, ConfigurableWorkingDir<S> {
    override fun withWorkingDir(
        dir: IDFile,
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
        dir: IDFile,
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
    fun execute(
        workingDir: IDFile?,
        env: Map<String, String>,
        args: Array<out String>
    ): ShellResult
}


fun interface ShellExecutorFactory {
    companion object {
        val DEFAULT = ShellExecutorFactory { saveOutput, verbosity, outLogger,errLogger ->
            DefaultShellExecutor(
                saveOutput = saveOutput,
                verbosity = verbosity,
                outLogger = outLogger,
                errLogger = errLogger
            )
        }
    }

    fun executor(
        saveOutput: Boolean,
        verbosity: ShellVerbosity,
        outLogger: Prints,
        errLogger: Prints
    ): ShellExecutor
}


class DefaultShellExecutor(
    private val saveOutput: Boolean,
    private val verbosity: ShellVerbosity,
    private val outLogger: Prints,
    private val errLogger: Prints
) : ShellExecutor {
    override fun execute(
        workingDir: IDFile?,
        env: Map<String, String>,
        args: Array<out String>
    ): ShellResult {
        val p = proc(
            wd = workingDir,
            args = args,
            env = env
        )
        return try {
            p.await(
                verbosity = verbosity,
                outLogger = outLogger,
                errLogger = errLogger,
                saveOutput = saveOutput
            )
        } finally {
            p.descendants().forEachOrdered {
                it.destroyForcibly()
            }
            p.destroyForcibly()
        }

    }
}


data class ExecReturner(
    override val programPathContext: ShellProgramPathContext = DEFAULT_MAC_PROGRAM_PATH_CONTEXT,
    private val verbosity: ShellVerbosity,
    private val workingDir: IDFile? = null,
    private val env: Map<String, String> = mapOf(),
    private val outLogger: Prints = SystemOutLogger.apply { includeTimeInfo = false },
    private val errLogger: Prints = SystemErrLogger.apply { includeTimeInfo = false },
    private val resultHandler: ShellResultHandler<ShellFullResult>? = null,
) : DirectableShell<String, ExecReturner>, ConfigurableShell<String, ExecReturner> {
    companion object {
        val SILENT by lazy { ExecReturner(verbosity = ShellVerbosity.SILENT) }
        val STREAM by lazy { ExecReturner(verbosity = ShellVerbosity.STREAM) }
        val STREAM_CHARS by lazy { ExecReturner(verbosity = ShellVerbosity.STREAM_CHARS) }
    }

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
        dir: IDFile,
        op: ExecReturner.() -> Unit
    ) = copy(workingDir = dir).apply(op)

    fun <R> withVerbosity(
        verbosity: ShellVerbosity,
        op: ExecReturner.() -> R
    ) = copy(verbosity = verbosity).run(op)

    override fun doNotPrintCommand(op: ExecReturner.() -> Unit) =
        copy(verbosity = verbosity.copy(doNotPrintArgs = true)).apply(op)

    override fun sendCommand(vararg args: String): String {
        return shell(
            *(args.strings()),
            verbosity = verbosity,
            outLogger = outLogger,
            errLogger = errLogger,
            resultHandler = resultHandler,
            workingDir = workingDir,
            env = env,
        )
    }
}


interface UnControlledCommand<R>
interface ShellProgram<R> : UnControlledCommand<R>, Commandable<R> {
    override fun sendCommand(vararg args: String): R
}

class SimpleShellProgram<R>(val shell: Commandable<R>, val program: String) : ShellProgram<R> {
    override fun sendCommand(vararg args: String): R {
        return shell.sendCommand(program, *args)
    }
}

abstract class ControlledShellProgram<R>(private val program: ShellProgram<R>) {
    constructor(program: String, shell: Commandable<R>) : this(SimpleShellProgram(shell = shell, program = program))

    protected fun sendCommand(vararg args: String): R {
        return program.sendCommand(*args)
    }
}


fun exec(
    wd: IDFolder?,
    vararg args: String
) = proc(
    wd,
    *args
).waitFor() == 0

fun shells(
    verbosity: ShellVerbosity = ShellVerbosity.SILENT,
    workingDir: IDFolder? = null,
    env: Map<String, String> = mapOf(),
    op: ExecReturner.() -> Unit
) {
    ExecReturner(
        verbosity = verbosity,
        workingDir = workingDir,
        env = env
    ).apply(op)
}


fun shell(
    vararg args: String,
    workingDir: IDFile? = null,
    env: Map<String, String> = mapOf(),
    verbosity: ShellVerbosity = Companion.DEFAULT,
    outLogger: Prints = DefaultLogger,
    errLogger: Prints = DefaultLogger,
    executorFactory: ShellExecutorFactory = ShellExecutorFactory.DEFAULT,
    resultHandler: ShellResultHandler<ShellFullResult>? = null
) = FullResultShellRunner(
    args = args,
    workingDir = workingDir,
    env = env,
    verbosity = verbosity,
    outLogger = outLogger,
    errLogger = errLogger,
    resultHandler = resultHandler,
    executorFactory = executorFactory
).run().output


fun streamingMemorySafeShells(
    verbosity: ShellVerbosity = Companion.SILENT,
    workingDir: IDFolder? = null,
    env: Map<String, String> = mapOf(),
    op: ExecStreamer.() -> Unit
) {
    ExecStreamer(
        verbosity = verbosity,
        workingDir = workingDir,
        env = env
    ).apply(op)
}


fun streamingMemorySafeShell(
    vararg args: String,
    workingDir: IDFile? = null,
    env: Map<String, String> = mapOf(),
    verbosity: ShellVerbosity = Companion.DEFAULT,
    logger: Prints = DefaultLogger,
    resultHandler: ShellResultHandler<ShellResult>? = null
) = MemSafeShellRunner(
    args = args,
    workingDir = workingDir,
    env = env,
    verbosity = verbosity,
    logger = logger,
    resultHandler = resultHandler
).run()


abstract class ShellRunner<S : ShellResult>(
    private vararg val args: String,
    private val workingDir: IDFile? = null,
    private val env: Map<String, String> = mapOf(),
    private val verbosity: ShellVerbosity = Companion.DEFAULT,
    private val outLogger: Prints = DefaultLogger,
    private val errLogger: Prints = DefaultLogger,
    private val executorFactory: ShellExecutorFactory = ShellExecutorFactory.DEFAULT,
    private val resultHandler: ShellResultHandler<S>? = null
) {
    protected abstract val saveOutput: Boolean
    fun run(): S {
        if (verbosity.printRunning) {
            if (verbosity.doNotPrintArgs) outLogger.println("running command (hidden args)")
            else outLogger.println("running command: ${args.joinToString(" ")}")
        }
        val result = executorFactory.executor(
            saveOutput = saveOutput,
            verbosity = verbosity,
            outLogger = outLogger,
            errLogger = errLogger
        ).execute(
            workingDir = workingDir,
            env = env,
            args = args
        )
        if (verbosity.explainOutput) outLogger.println("output: ${(result as? ShellFullResult)?.output}")

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
    workingDir: IDFile? = null,
    env: Map<String, String> = mapOf(),
    verbosity: ShellVerbosity = Companion.DEFAULT,
    logger: Prints = DefaultLogger,
    resultHandler: ShellResultHandler<ShellResult>? = null
) : ShellRunner<ShellResult>(
    args = args,
    workingDir = workingDir,
    env = env,
    verbosity = verbosity,
    outLogger = logger,
    errLogger = logger,
    resultHandler = resultHandler
) {
    init {
        require(!verbosity.explainOutput)
    }

    override val saveOutput = false
}

class FullResultShellRunner(
    vararg args: String,
    workingDir: IDFile? = null,
    env: Map<String, String> = mapOf(),
    verbosity: ShellVerbosity = Companion.DEFAULT,
    errLogger: Prints = DefaultLogger,
    outLogger: Prints = DefaultLogger,
    executorFactory: ShellExecutorFactory = ShellExecutorFactory.DEFAULT,
    resultHandler: ShellResultHandler<ShellFullResult>? = null
) : ShellRunner<ShellFullResult>(
    args = args,
    workingDir = workingDir,
    env = env,
    verbosity = verbosity,
    errLogger = errLogger,
    outLogger = outLogger,
    resultHandler = resultHandler,
    executorFactory = executorFactory
) {
    override val saveOutput = true
}


data class ExecStreamer(
    override val programPathContext: ShellProgramPathContext = DEFAULT_MAC_PROGRAM_PATH_CONTEXT,
    private val verbosity: ShellVerbosity,
    private val workingDir: IDFile? = null,
    private val env: Map<String, String> = mapOf(),
    private val logger: Prints = SystemOutLogger.apply { includeTimeInfo = false },
    private val resultHandler: ShellResultHandler<ShellResult>? = null
) : DirectableShell<ShellResult, ExecStreamer>, ConfigurableShell<ShellResult, ExecStreamer> {
    companion object {
        val SILENT by lazy { ExecStreamer(verbosity = ShellVerbosity.SILENT) }
        val STREAM by lazy { ExecStreamer(verbosity = ShellVerbosity.STREAM) }
        val STREAM_CHARS by lazy { ExecStreamer(verbosity = ShellVerbosity.STREAM_CHARS) }
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
        dir: IDFile,
        op: ExecStreamer.() -> Unit
    ) = copy(workingDir = dir).apply(op)

    fun <R> withVerbosity(
        verbosity: ShellVerbosity,
        op: ExecStreamer.() -> R
    ) = copy(verbosity = verbosity).run(op)

    override fun doNotPrintCommand(op: ExecStreamer.() -> Unit) =
        copy(verbosity = verbosity.copy(doNotPrintArgs = true)).apply(op)

    override fun sendCommand(vararg args: String): ShellResult {
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


class CommandReturner(
    override val programPathContext: ShellProgramPathContext = DEFAULT_MAC_PROGRAM_PATH_CONTEXT
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
        return rawWithNoEscaping()
    }

    fun asArray() = commands.toTypedArray()

    infix fun pipedTo(consumer: Command) = Command(commands + "|" + consumer.commands)

    infix fun pipedToFile(file: FilePath) = Command(commands + ">" + file.filePath)

}