package matt.shell.test.jcommon

import matt.lang.anno.optin.IncubatingMattCode
import matt.shell.common.Command
import matt.shell.commonj.context.escape.EscapeWithQuotes
import matt.shell.commonj.context.escape.NoEscaping
import matt.shell.commonj.context.escape.None
import matt.shell.commonj.context.escape.unix.UnixEscapeDoubleQuotedStringContext
import matt.shell.commonj.context.escape.unix.UnixEscapeUnquotedStringContext
import kotlin.test.Test


@OptIn(IncubatingMattCode::class)
class ShellJCommonTests() {
    @Test
    fun instantiateClasses() {
        Command(listOf("a"))
        EscapeWithQuotes(NoEscaping)
    }

    @Test
    fun initObjects() {
        UnixEscapeUnquotedStringContext
        UnixEscapeDoubleQuotedStringContext
        None
    }
}
