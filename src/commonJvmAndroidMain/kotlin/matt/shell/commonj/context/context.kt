package matt.shell.commonj.context

import matt.lang.context.DEFAULT_LINUX_PROGRAM_PATH_CONTEXT
import matt.lang.context.DEFAULT_WINDOWS_PROGRAM_PATH_CONTEXT
import matt.lang.context.ShellProgramPathContext
import matt.lang.platform.common.HasOs
import matt.lang.platform.common.OsEnum
import matt.lang.platform.common.OsEnum.Windows
import matt.lang.shutdown.preaper.ProcessReaper
import matt.shell.common.context.DefaultMacExecutionContext
import matt.shell.common.context.KnownShellExecutionContext
import matt.shell.common.context.ShellExecutionContext
import matt.shell.common.context.shell.Bash
import matt.shell.common.context.shell.Powershell
import matt.shell.common.context.shell.ShellLanguage
import matt.shell.common.context.shell.UnixDirectCommandsOnly


interface ReapingShellExecutionContext : ShellExecutionContext, ProcessReaper

class ReapingShellExecutionContextImpl(
    val shellExecutionContext: ShellExecutionContext,
    val processReaper: ProcessReaper
) : ReapingShellExecutionContext, ShellExecutionContext by shellExecutionContext, ProcessReaper by processReaper

fun ShellExecutionContext.withReaper(processReaper: ProcessReaper) =
    ReapingShellExecutionContextImpl(this, processReaper)

fun ProcessReaper.withShellExecutionContext(shellExecutionContext: ShellExecutionContext) =
    ReapingShellExecutionContextImpl(shellExecutionContext, this)


class UnknownShellExecutionContext(
    override val language: ShellLanguage
) : ShellExecutionContext {
    override val shellProgramPathContext = null

    override val inSingularityContainer = null
    override val inSlurmJob = null
    override val needsModules = null

    override fun inSingularity() =
        PartiallyKnownExecutionContext(
            inSingularityContainer = true,
            needsModules = false,
            language = language
        )

    override fun inSlurmJob() =
        PartiallyKnownExecutionContext(

            shellProgramPathContext = DEFAULT_LINUX_PROGRAM_PATH_CONTEXT,
            inSlurmJob = true,
            language = language

        )

    override fun withBash() = TODO()
}

data class PartiallyKnownExecutionContext(
    override val shellProgramPathContext: ShellProgramPathContext? = null,
    override val inSingularityContainer: Boolean? = null,
    override val inSlurmJob: Boolean? = null,
    override val needsModules: Boolean? = null,
    override val language: ShellLanguage
) : ShellExecutionContext {
    override fun inSingularity(): ShellExecutionContext =
        copy(
            inSingularityContainer = true,
            needsModules = false
        )

    override fun inSlurmJob() =
        copy(
            shellProgramPathContext = DEFAULT_LINUX_PROGRAM_PATH_CONTEXT,
            inSlurmJob = true
        )

    override fun withBash() = copy(language = Bash)
}



val DefaultLinuxExecutionContext by lazy {
    PartiallyKnownExecutionContext(
        shellProgramPathContext = DEFAULT_LINUX_PROGRAM_PATH_CONTEXT,
        language = UnixDirectCommandsOnly
    )
}

val DefaultWindowsExecutionContext by lazy {
    KnownShellExecutionContext(
        shellProgramPathContext = DEFAULT_WINDOWS_PROGRAM_PATH_CONTEXT,
        inSingularityContainer = false,
        inSlurmJob = false,
        needsModules = false,
        language = Powershell
    )
}


val HasOs.knownShellContextFromOs
    get() =
        when (os) {
            OsEnum.Linux ->
                PartiallyKnownExecutionContext(
                    shellProgramPathContext = DEFAULT_LINUX_PROGRAM_PATH_CONTEXT,
                    language = UnixDirectCommandsOnly
                )

            OsEnum.Mac   -> DefaultMacExecutionContext
            Windows      -> DefaultWindowsExecutionContext
        }
