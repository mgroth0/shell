package matt.shell.proc

import matt.async.thread.namedThread
import matt.lang.consume
import matt.lang.file.toJFile
import matt.lang.function.Op
import matt.lang.model.file.AnyResolvableFilePath
import matt.lang.seq.charSequence
import matt.lang.shutdown.preaper.ProcessReaper
import matt.log.DefaultLogger
import matt.model.flowlogic.latch.SimpleThreadLatch
import matt.model.op.prints.Prints
import matt.prim.str.joinWithSpaces
import matt.shell.PrintInSeq.CHARS
import matt.shell.PrintInSeq.LINES
import matt.shell.PrintInSeq.NO
import matt.shell.ShellVerbosity
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
        if (isAlive) {
            /*getting descendents turns out to be super expensive with large numbers of processes. Hits memory allocation hard and throws memory errors when we need to go through thousands of processes for example for imagemagick

            ... point is avoid doing the things below whenever possible


            behavior wise, this opens the door for sub-subprocesses to stay alive. But now I have a new technique to deal with that kind of issue, which is a better technique anyway.

             */
            descendants().forEachOrdered {
                it.destroyForcibly()
            }
            destroyForcibly()
        }
    }
}

context(ProcessReaper)
fun proc(
    wd: AnyResolvableFilePath?,
    vararg args: String,
    env: Map<String, String> = mapOf()
): Process {
    /*Runtime.exec is not quite deprecated, but ProcessBuilder is officially the recommended approach. The only reason Runtime.exec still exists is because so much old code uses it, and it is harmless. However, it might lack some of the performance and organizational perks of ProcessBuilder*/
    val processBuilder = ProcessBuilder()
    processBuilder.directory(wd?.toJFile())
    processBuilder.environment().putAll(env)
    processBuilder.command(*args)
    try {
        val p = processBuilder.start()
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
    saveOutput: Boolean,
): ShellResult {
    var err = ""

    /*MUST USE THREAD. IF IS TRY TO DO THIS SEQUENTIALLY, SOMETIMES EITHER ERR OR STDOUT IS SO LARGE THAT IT PREVENTS THE OTHER ONE FROM COMING THROUGH, CAUSING BLOCKING IF I TRY TO GET EACH IN SEQUENCE.*/

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
        if (verbosity.outBeforeErr) latch.await()
        if (saveOutput) err = savingInputOp(errorReader(), errLogger)
        else inputOp(errorReader(), errLogger)
    }
    try {
        if (saveOutput) out = savingInputOp(inputReader(), outLogger)
        else inputOp(inputReader(), outLogger)
    } finally {
        latch.open()
    }
    t.join()
    val code = waitFor()

    return if (saveOutput) ShellFullResult(code = code, std = out, err = err)
    else ShellResult(code = code)
}

val Process.streams: List<InputStream> get() = listOf(inputStream, errorStream)
fun Process.forEachOutChar(op: (String) -> Unit) = inputStream.bufferedReader().forEachChar {
    op(it)
}

fun Process.forEachErrChar(op: (String) -> Unit) = errorStream.bufferedReader().forEachChar {
    op(it)
}


fun Process.whenDead(
    daemon: Boolean = true,
    op: Op
) {
    namedThread(isDaemon = daemon, name = "When Dead Thread") {
        waitFor()
        op()
    }
}





