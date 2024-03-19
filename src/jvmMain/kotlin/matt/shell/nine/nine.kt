package matt.shell.nine

import matt.lang.shutdown.preaper.ProcessInter
import matt.shell.unblock.waitForSuspending
import java.io.BufferedReader
import java.io.InputStream
import java.io.OutputStream


class Java9ProcessWrapper(
    private val process: Process
): ProcessInter {
    override val outputStream: OutputStream
        get() = process.outputStream
    override val errorStream: InputStream
        get() = process.errorStream
    override val inputStream: InputStream
        get() = process.inputStream

    override fun pid(): Long = process.pid()

    override fun errorReader(): BufferedReader = process.errorReader()

    override suspend fun waitFor(): Int = process.waitForSuspending()
}
