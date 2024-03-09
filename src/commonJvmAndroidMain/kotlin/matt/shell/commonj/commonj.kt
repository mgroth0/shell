package matt.shell.commonj

import matt.lang.anno.SeeURL
import matt.shell.common.Command
import matt.shell.common.Shell
import matt.shell.common.context.ShellExecutionContext

class CommandReturner(
    override val executionContext: ShellExecutionContext
) : Shell<Command> {
    override fun sendCommand(vararg args: String) = Command(args.map { it.toString() })
}


abstract class AndroidShell<R>: Shell<R> {
    /*activity manager*/
    fun am(vararg args: String) = sendCommand("am", *args)

    @SeeURL("https://stackoverflow.com/a/12274218/6596010")
    fun forceStop(appID: String) = am("force-stop", appID)
}
