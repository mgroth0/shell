package matt.shell.context.escape.win

import matt.lang.anno.SeeURL
import matt.lang.anno.optin.IncubatingMattCode
import matt.shell.context.escape.EscapeContext

sealed interface WindowsEscapeContext : EscapeContext

@IncubatingMattCode(basicWorkNeeded = "try to make it work in all cases")
@SeeURL("https://ss64.com/nt/syntax-esc.html")
object WindowsCommandPromptEscapeContext : WindowsEscapeContext {
    override val charsToEscape = listOf(' ', '=')
    override val escapeChar = '^'
}


@IncubatingMattCode(basicWorkNeeded = "try to make it work in all cases")
@SeeURL("https://www.rlmueller.net/PowerShellEscape.htm#:~:text=The%20PowerShell%20escape%20character%20is,interactively%2C%20or%20running%20PowerShell%20scripts.")
@SeeURL("https://ss64.com/ps/syntax-esc.html")
@SeeURL("https://www.google.com/search?q=esca%5Be+chracter+windows+powershell&oq=esca%5Be+chracter+windows+powershell&aqs=chrome..69i57.6904j0j1&sourceid=chrome&ie=UTF-8")
object WindowsPowershellEscapeContext : WindowsEscapeContext {
    override val charsToEscape = listOf(
        ' ',
        '=',

        @SeeURL("https://stackoverflow.com/questions/71395217/escape-the-period-character-in-a-command-flag-within-a-powershell-script")
        @SeeURL("https://github.com/PowerShell/PowerShell/issues/6291")
        /* Getting this when including "--java-options", "-Dkotlinx.coroutines.debug" in command:

            jdk.jpackage.internal.PackagerException: Error: Invalid Option: [.coroutines.debug]


        * */
        '.'
    )
    override val escapeChar = '`'
}



