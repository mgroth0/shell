package matt.shell.commands.uname

import matt.lang.os
import matt.shell.Shell
import matt.shell.win.WINDOWS_CMD_BASH_PREFIX
import kotlin.io.path.Path
import kotlin.io.path.exists


fun Shell<String>.unameForThisRuntime(): String {
    val commandLine = correctUnameCommandLineForThisRuntime()
    return sendCommand(*commandLine.toTypedArray()).trim()
}

private fun correctUnameCommandLineForThisRuntime(): List<String> {
    val commandLine = if ("Windows" in os) listOf(*WINDOWS_CMD_BASH_PREFIX, correctUnamePathForThisRuntime())
    else listOf(correctUnamePathForThisRuntime())
    return commandLine + "-m"
}

private fun correctUnamePathForThisRuntime(): String {
    return if ("Windows" in os) "/usr/bin/uname" else listOf(
        "/usr/bin/uname", "/bin/uname" /*vagrant, singularity*/
    ).first { Path(it).exists() }
}