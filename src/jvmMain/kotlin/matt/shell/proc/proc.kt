package matt.shell.proc

import matt.async.thread.namedThread
import matt.lang.common.consume
import matt.lang.file.toJFile
import matt.lang.function.Consume
import matt.lang.j.plusAssign
import matt.lang.model.file.AnyResolvableFilePath
import matt.lang.seq.charSequence
import matt.lang.shutdown.preaper.ProcessDestiny
import matt.lang.shutdown.preaper.ProcessReaper
import matt.log.j.DefaultLogger
import matt.model.flowlogic.latch.j.SimpleThreadLatch
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

context(ProcessReaper)
fun proc(
    wd: AnyResolvableFilePath?,
    vararg args: String,
    destroyRecursiveSubprocesses: Boolean = false,
    env: Map<String, String> = mapOf()
): ProcessDestiny {
    /*Runtime.exec is not quite deprecated, but ProcessBuilder is officially the recommended approach. The only reason Runtime.exec still exists is that so much old code uses it, and it is harmless. However, it might lack some of the performance and organizational perks of ProcessBuilder*/
    val processBuilder =
        ProcessBuilder()
            .directory(wd?.toJFile())
            .apply {
                environment().putAll(env)
            }
            .command(*args)
    try {
        val p = processBuilder.start()
        val dest = registerProcessShutdownTask(p, andRecursiveDescendants = destroyRecursiveSubprocesses)
        return dest
    } catch (e: IOException) {
        val fullCommand = StringBuilder()
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

    /*
    Reminder:
        - stdout and stderr can block each other (I guess if some buffer reaches a capacity?).
        - Therefore, they must be read in separate threads.
     */

    var err = ""
    var out = ""

    fun savingInputOp(
        reader: BufferedReader,
        streamLogger: Prints
    ) = when (verbosity.printInSequence) {
        NO    -> reader.readText()
        LINES ->
            reader.lineSequence().onEach {
                streamLogger.println(it)
            }.joinToString("\n")

        CHARS ->
            reader.charSequence().onEach {
                streamLogger.print(it)
            }.joinToString("")
    }

    fun inputOp(
        reader: BufferedReader,
        streamLogger: Prints
    ) = when (verbosity.printInSequence) {
        NO    -> reader.charSequence().consume()

        LINES ->
            reader.lineSequence().onEach {
                streamLogger.println(it)
            }.consume()

        CHARS ->
            reader.charSequence().onEach {
                streamLogger.print(it)
            }.consume()
    }


    val latch = SimpleThreadLatch()
    val t =
        namedThread(name = "errorReader") {
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

fun Process.forEachOutChar(op: Consume<String>) = inputReader().forEachChar(op)
fun Process.forEachErrChar(op: Consume<String>) = errorReader().forEachChar(op)


