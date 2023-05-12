package matt.shell

import matt.lang.go
import matt.log.DefaultLogger
import matt.log.SystemOutLogger
import matt.model.data.file.FilePath
import matt.model.data.file.IDFile
import matt.model.data.file.IDFolder
import matt.model.op.prints.Prints
import matt.prim.str.strings
import matt.service.MattService
import matt.shell.PrintInSeq.CHARS
import matt.shell.PrintInSeq.LINES
import matt.shell.PrintInSeq.NO
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
interface Shell<R : Any?> : MattService {
    fun sendCommand(vararg args: Any): R
    val FilePath.pathOp: String get() = filePath
}


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
        val DEFAULT = ShellExecutorFactory { saveOutput, verbosity, logger ->
            DefaultShellExecutor(
                saveOutput = saveOutput,
                verbosity = verbosity,
                logger = logger
            )
        }
    }

    fun executor(
        saveOutput: Boolean,
        verbosity: ShellVerbosity,
        logger: Prints
    ): ShellExecutor
}


class DefaultShellExecutor(
    private val saveOutput: Boolean,
    private val verbosity: ShellVerbosity,
    private val logger: Prints
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
        return p.await(
            verbosity = verbosity,
            logger = logger,
            saveOutput = saveOutput
        )
    }
}


data class ExecReturner(
    private val verbosity: ShellVerbosity,
    private val workingDir: IDFile? = null,
    private val env: Map<String, String> = mapOf(),
    private val logger: Prints = SystemOutLogger.apply { includeTimeInfo = false },
    private val resultHandler: ShellResultHandler<ShellFullResult>? = null
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

    override fun sendCommand(vararg args: Any): String {
        return shell(
            *(args.strings()),
            verbosity = verbosity,
            logger = logger,
            resultHandler = resultHandler,
            workingDir = workingDir,
            env = env,
        )
    }
}




interface ControlledCommand<R>
interface Command<R> : ControlledCommand<R> {
    fun sendCommand(vararg args: String): R
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
    logger: Prints = DefaultLogger,
    executorFactory: ShellExecutorFactory = ShellExecutorFactory.DEFAULT,
    resultHandler: ShellResultHandler<ShellFullResult>? = null
) = FullResultShellRunner(
    args = args,
    workingDir = workingDir,
    env = env,
    verbosity = verbosity,
    logger = logger,
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
    private val logger: Prints = DefaultLogger,
    private val executorFactory: ShellExecutorFactory = ShellExecutorFactory.DEFAULT,
    private val resultHandler: ShellResultHandler<S>? = null
) {
    protected abstract val saveOutput: Boolean
    fun run(): S {
        if (verbosity.printRunning) {
            if (verbosity.doNotPrintArgs) logger.println("running command (hidden args)")
            else logger.println("running command: ${args.joinToString(" ")}")
        }
        val result = executorFactory.executor(
            saveOutput = saveOutput,
            verbosity = verbosity,
            logger = logger
        ).execute(
            workingDir = workingDir,
            env = env,
            args = args
        )
        if (verbosity.explainOutput) logger.println("output: ${(result as? ShellFullResult)?.output}")

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
    logger = logger,
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
    logger: Prints = DefaultLogger,
    executorFactory: ShellExecutorFactory = ShellExecutorFactory.DEFAULT,
    resultHandler: ShellResultHandler<ShellFullResult>? = null
) : ShellRunner<ShellFullResult>(
    args = args,
    workingDir = workingDir,
    env = env,
    verbosity = verbosity,
    logger = logger,
    resultHandler = resultHandler,
    executorFactory = executorFactory
) {
    override val saveOutput = true
}



data class ExecStreamer(
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

    override fun sendCommand(vararg args: Any): ShellResult {
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

