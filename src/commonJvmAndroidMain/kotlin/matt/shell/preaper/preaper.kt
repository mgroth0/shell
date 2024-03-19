package matt.shell.preaper

import matt.lang.jexpects.processDescendants
import matt.lang.jexpects.processErrorReader
import matt.lang.jexpects.processPid
import matt.lang.service.ThreadProvider
import matt.lang.shutdown.RushableShutdownTask
import matt.lang.shutdown.TypicalShutdownContext
import matt.lang.shutdown.preaper.ProcessDestiny
import matt.lang.shutdown.preaper.ProcessHandleInter
import matt.lang.shutdown.preaper.ProcessReaper
import matt.shell.unblock.waitForSuspending
import java.io.BufferedReader
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.TimeoutException
import kotlin.time.Duration.Companion.milliseconds


class ProcessReaperImpl(
    threadProvider: ThreadProvider,
    private val shutdownExecutor: TypicalShutdownContext
) : ProcessReaper, TypicalShutdownContext by shutdownExecutor {


    private companion object {
        private val CHECK_INTERVAL = 100.milliseconds.inWholeMilliseconds

        /*This is very important for scenarios where I am spawning large numbers of processes. Given the fast spawn rate, I need to clean them up fast too or else I will get memory allocation errors*/
        private val SHORT_CHECK_INTERVAL = 10.milliseconds.inWholeMilliseconds
    }

    private val subProcessQueue = ConcurrentLinkedQueue<ProcessDestinyImpl>()
    private val subProcesses = mutableSetOf<ProcessDestinyImpl>()
    private var startedReaping = false

    init {
        val monitor = Object()
        threadProvider.newThread(isDaemon = true, name = "ProcessCheckerDaemon") {
            var useShortWait = true
            do {
                val didStartReaping =
                    synchronized(monitor) {
                        if (startedReaping) return@synchronized true
                        monitor.wait(
                            if (useShortWait) SHORT_CHECK_INTERVAL else CHECK_INTERVAL
                        )
                        startedReaping
                    }
                val shouldContinue =
                    if (didStartReaping) false
                    else {
                        do {
                            val p = subProcessQueue.poll()
                            val didAdd = if (p != null) subProcesses.add(p) else false
                        } while (didAdd)

                        val size = subProcesses.size
                        useShortWait =
                            if (size > 0) {
                                val itr = subProcesses.iterator()
                                repeat(size) {
                                    val d = itr.next()
                                    if (!d.process.isAlive) {
                                        d.shutdownTask.cancel()
                                        itr.remove()
                                    }
                                }
                                true
                            } else false
                        true
                    }
            } while (shouldContinue)
        }
        shutdownExecutor.duringShutdown {
            startedReaping = true
            synchronized(monitor) {
                monitor.notify()
            }
        }
    }


    /*Warning: andRecursiveDescendants can have large memory impact if there is a large number of processes that have subprocesses*/
    override fun registerProcessShutdownTask(process: Process, andRecursiveDescendants: Boolean): ProcessDestiny {
        val shutdownTask =
            shutdownExecutor.duringShutdown {

                val descendantsSnapshot: List<ProcessHandleInter>  = (processDescendants(process)).toList()


                process.destroy()
                val didExit = process.waitFor(10, SECONDS)
                if (!didExit) {
                    println(
                        "Warning: $process did not exit gracefully within 10 seconds (likely received sigterm), and must be destroyed forcibly (with sigkill)"
                    )
                    process.destroyForcibly().waitFor()
                }

                /*Allow parent process a chance to handle ending its own subprocesses first.*/

                if (andRecursiveDescendants) {
                    descendantsSnapshot.forEach { descendant ->
                        descendant.destroy()
                        try {
                            descendant.onExit().get(10, SECONDS)
                        } catch (e: TimeoutException) {
                            println(
                                "Warning: Descendant $descendant did not exit gracefully within 10 seconds (likely received sigterm), and must be destroyed forcibly (with sigkill)"
                            )
                            check(descendant.destroyForcibly())
                            checkNotNull(descendant.onExit().get(3, SECONDS))
                        }
                    }
                }
            }
        val destiny = ProcessDestinyImpl(process, shutdownTask)
        subProcessQueue.add(destiny)
        return destiny
    }



    private class ProcessDestinyImpl(
        override val process: Process,
        val shutdownTask: RushableShutdownTask
    ): ProcessDestiny {
        override fun runShutdownNowInsteadOfLater() {
            shutdownTask.runNowIfScheduledInsteadOfLater()
        }

        override val outputStream: OutputStream get() = process.outputStream
        override val inputStream: InputStream get() = process.inputStream
        override val errorStream: InputStream get() = process.errorStream
        override fun pid(): Long = processPid(process)
        override fun errorReader(): BufferedReader = processErrorReader(process)
        override suspend fun waitFor(): Int = process.waitForSuspending()
    }
}
