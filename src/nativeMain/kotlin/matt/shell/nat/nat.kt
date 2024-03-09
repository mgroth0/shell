package matt.shell.nat

import matt.prim.str.joinWithSpaces
import matt.shell.common.Shell
import matt.shell.common.context.ShellExecutionContext
import platform.posix.system


class NativeShell(
    override val executionContext: ShellExecutionContext
): Shell<Unit> {
    override fun sendCommand(vararg args: String) {
        check(system(args.joinWithSpaces()) == 0)
    }
}
