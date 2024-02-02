package matt.shell.context.escape.unix

import matt.lang.anno.SeeURL
import matt.lang.anno.optin.IncubatingMattCode
import matt.shell.context.escape.EscapeContext


@SeeURL("https://stackoverflow.com/a/6697781/6596010")
@IncubatingMattCode(basicWorkNeeded = "try to make it work in all cases")
object UnixEscapeUnquotedStringContext : EscapeContext {
    override val escapeChar = '\\'
    override val charsToEscape = listOf(
        ' ',

        /*yes curly braces were causing bugs.. Open a terminal and write 'echo {a,b}'*/
        '{',
        '}',

        '"',
        '\\',
        '$',
    )
}


@SeeURL("https://superuser.com/questions/1276707/insert-quote-into-string-variable-in-bash")
@IncubatingMattCode(basicWorkNeeded = "try to make it work in all cases")
object UnixEscapeDoubleQuotedStringContext : EscapeContext {
    override val escapeChar = '\\'
    override val charsToEscape = listOf(
        '"',
        '\\',
        '$',
    )
}
