package matt.shell.common.context.shell

sealed interface ShellLanguage

sealed interface UnixDirectCommands : ShellLanguage
data object UnixDirectCommandsOnly : UnixDirectCommands
sealed interface UnixShellLangauge : UnixDirectCommands
sealed interface BourneLikeShell : UnixShellLangauge
sealed interface BourneCompliantShell : BourneLikeShell
sealed interface PosixCompliantShell : UnixShellLangauge
sealed interface BashLikeShell : BourneLikeShell


data object Bash : BashLikeShell
data object BashPosixMode : BashLikeShell, PosixCompliantShell
data object BashShMode : BourneCompliantShell

data object Zsh : BashLikeShell
data object ZshPosixMode : BashLikeShell, PosixCompliantShell

data object Dash : PosixCompliantShell, BourneLikeShell

data object Powershell : ShellLanguage


