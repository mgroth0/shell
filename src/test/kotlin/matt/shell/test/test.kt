package matt.shell.test


import matt.shell.commands.dnf.MicroDnf
import matt.shell.context.DefaultMacExecutionContext
import matt.shell.execReturners
import matt.test.JupiterTestAssertions.assertRunsInOneMinute
import kotlin.test.Test

class ShellTests {

    @Test
    fun instantiateClasses() = assertRunsInOneMinute {

        with(DefaultMacExecutionContext) {
            MicroDnf(
                execReturners.stream
            )
        }

    }

}