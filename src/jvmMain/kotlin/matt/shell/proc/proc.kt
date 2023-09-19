package matt.shell.proc

import matt.async.thread.namedThread
import matt.lang.RUNTIME
import matt.lang.consume
import matt.lang.file.toJFile
import matt.lang.function.Op
import matt.lang.model.file.FilePath
import matt.lang.seq.charSequence
import matt.lang.shutdown.preaper.ProcessReaper
import matt.log.DefaultLogger
import matt.log.warn.warn
import matt.model.flowlogic.latch.SimpleThreadLatch
import matt.model.op.prints.Prints
import matt.prim.str.joinWithSpaces
import matt.prim.str.lower
import matt.shell.PrintInSeq.CHARS
import matt.shell.PrintInSeq.LINES
import matt.shell.PrintInSeq.NO
import matt.shell.ShellVerbosity
import matt.shell.commands.kill.kill
import matt.shell.context.ReapingShellExecutionContext
import matt.shell.execReturners
import matt.shell.proc.ProcessKillSignal.SIGINT
import matt.shell.proc.ProcessKillSignal.SIGKILL
import matt.shell.proc.ProcessKillSignal.SIGTERM
import matt.shell.report.NonZeroShellResult
import matt.shell.report.ShellFullResult
import matt.shell.report.ShellResult
import matt.stream.forEachChar
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream

inline fun <R> Process.use(op: () -> R): R {
    try {
        return op()
    } finally {
        descendants().forEachOrdered {
            it.destroyForcibly()
        }
        destroyForcibly()
    }
}

context(ProcessReaper)
fun proc(
    wd: FilePath?,
    vararg args: String,
    env: Map<String, String> = mapOf()
): Process {
    val envP = env.map {
        it.key + "=" + it.value
    }.toTypedArray()
    try {
        val p = if (wd == null) RUNTIME.exec(
            args, envP
        ) else RUNTIME.exec(
            args, envP, wd.toJFile()
        )
        ensureProcessEndsAtShutdown(p)
        return p
    } catch (e: IOException) {
        var fullCommand = ""
        if (wd != null) {
            fullCommand += "cd ${wd.path}; "
        }
        env.forEach {
            fullCommand += "${it.key}=${it.value} "
        }
        fullCommand += args.joinWithSpaces()
        println("COPY AND PASTE THIS TO REPLICATE IN TERMINAL: $fullCommand")
        throw e
    }
}

internal fun Process.await(
    verbosity: ShellVerbosity,
    outLogger: Prints = DefaultLogger,
    errLogger: Prints = DefaultLogger,
    saveOutput: Boolean
): ShellResult {


    var err =
        ""/*MUST USE THREAD. IF IS TRY TO DO THIS SEQUENTIALLY, SOMETIMES EITHER ERR OR STDOUT IS SO LARGE THAT IT PREVENTS THE OTHER ONE FROM COMING THROUGH, CAUSING BLOCKING IF I TRY TO GET EACH IN SEQUENCE.*/

    var out = ""

    fun savingInputOp(
        reader: BufferedReader,
        streamLogger: Prints
    ) = when (verbosity.printInSequence) {
        NO    -> reader.readText()
        LINES -> reader.lineSequence().onEach {
            streamLogger.println(it)
        }.joinToString("\n")

        CHARS -> reader.charSequence().onEach {
            streamLogger.print(it)
        }.joinToString("")
    }

    fun inputOp(
        reader: BufferedReader,
        streamLogger: Prints
    ) = when (verbosity.printInSequence) {
        NO    -> reader.charSequence().consume()

        LINES -> reader.lineSequence().onEach {
            streamLogger.println(it)
        }.consume()

        CHARS -> reader.charSequence().onEach {
            streamLogger.print(it)
        }.consume()
    }


    val latch = SimpleThreadLatch()
    val t = namedThread(name = "errorReader") {
        if (verbosity.outBeforeErr) {
            latch.await()
        }
        if (saveOutput) {
            err = savingInputOp(errorReader(), errLogger)
        } else {
            inputOp(errorReader(), errLogger)
        }

    }


    try {
        if (saveOutput) {
            out = savingInputOp(inputReader(), outLogger)
        } else {
            inputOp(inputReader(), outLogger)
        }
    } finally {
        latch.open()
    }


    t.join()
    val code = waitFor()

    return if (saveOutput) {
        ShellFullResult(code = code, std = out, err = err)
    } else {
        ShellResult(code = code)
    }


    //  return out + err
    /*
    streams.joinToString("") {
      it.bufferedReader().lines().toList().joinToString("\n")
    }
    */
}

val Process.streams: List<InputStream> get() = listOf(inputStream, errorStream)
fun Process.forEachOutChar(op: (String) -> Unit) = inputStream.bufferedReader().forEachChar {
    op(it)
}

fun Process.forEachErrChar(op: (String) -> Unit) = errorStream.bufferedReader().forEachChar {
    op(it)
}

//context (ShellExecutionContext, ShutdownExecutor)
//fun Process.scheduleShutdownKill() {
//    val processPid = pid()
//    duringShutdown {
//        if (isAlive) {
//            /*IntelliJ seems to kill Sys.out or stop reading it or whatever during the shutdown process, so this won't be seen in the console if run through an IntelliJ Gradle Run Action*/
//            println("killing process ($processPid)")
//            kill(SIGKILL)
//            println("killed process")
//        } else {
//            println("blender process ($processPid) is already dead")
//        }
//    }
//}

fun Process.whenDead(
    daemon: Boolean = true,
    op: Op
) {
    namedThread(isDaemon = daemon, name = "When Dead Thread") {
        waitFor()
        op()
    }
}

enum class ProcessKillSignal {
    SIGTERM, SIGINT, SIGKILL
}

@JvmInline
value class Pid(internal val id: Long) {

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

fun Process.myPid() = Pid(pid())
context (ReapingShellExecutionContext)
fun Process.kill(signal: ProcessKillSignal = SIGKILL) = myPid().kill(signal)

context (ReapingShellExecutionContext)
fun Process.interrupt() = kill(SIGINT)