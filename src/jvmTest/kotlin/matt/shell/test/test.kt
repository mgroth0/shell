package matt.shell.test


import matt.shell.commands.dnf.MicroDnf
import matt.shell.common.context.DefaultMacExecutionContext
import matt.shell.commonj.context.withReaper
import matt.shell.execReturners
import matt.test.Tests
import kotlin.test.Test

class ShellTests: Tests() {

    @Test
    fun instantiateClasses() {
        with(DefaultMacExecutionContext.withReaper(this)) {
            MicroDnf(
                execReturners.stream
            )
        }
    }
}
