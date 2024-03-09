package matt.shell.commands.uname

import matt.lang.platform.common.Windows
import matt.lang.platform.os.OS
import matt.shell.command
import matt.shell.commands.bash.bashC
import matt.shell.common.Shell
import matt.shell.commonj.context.DefaultWindowsExecutionContext
import kotlin.io.path.Path
import kotlin.io.path.exists

/*Somehow this didn't work once when I launched a second gradle daemon with a deifferent env or whatever. How though? All the paths are absolute... investigate*/


fun Shell<String>.unameForThisRuntime(): String {
    val commandLine = correctUnameCommandLineForThisRuntime()
    return sendCommand(*commandLine.toTypedArray()).trim()
}

private fun correctUnameCommandLineForThisRuntime(): List<String> {
    val commandLine =
        if (OS == Windows) DefaultWindowsExecutionContext.command.bashC {
            sendCommand(correctUnamePathForThisRuntime())
        }.asArray().toList() else listOf(correctUnamePathForThisRuntime())

    return commandLine + "-m"
}

private fun correctUnamePathForThisRuntime(): String =
    if (OS == Windows) "/usr/bin/uname" else listOf(
        "/usr/bin/uname", "/bin/uname" /*vagrant, singularity*/
    ).first { Path(it).exists() }
