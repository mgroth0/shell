package matt.shell.test


import matt.shell.commands.dnf.MicroDnf
import matt.shell.context.DefaultMacExecutionContext
import matt.shell.context.withReaper
import matt.shell.execReturners
import matt.test.Tests
import kotlin.test.Test

class ShellTests: Tests() {

    @Test
    fun instantiateClasses() = assertRunsInOneMinute {

        with(DefaultMacExecutionContext.withReaper(this)) {
            MicroDnf(
                execReturners.stream
            )
        }

    }

}
