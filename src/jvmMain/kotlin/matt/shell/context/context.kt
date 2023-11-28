package matt.shell.context

import matt.lang.context.DEFAULT_LINUX_PROGRAM_PATH_CONTEXT
import matt.lang.context.DEFAULT_MAC_PROGRAM_PATH_CONTEXT
import matt.lang.context.DEFAULT_WINDOWS_PROGRAM_PATH_CONTEXT
import matt.lang.context.ShellProgramPathContext
import matt.lang.platform.HasOs
import matt.lang.platform.OsEnum
import matt.lang.platform.OsEnum.Windows
import matt.lang.shutdown.preaper.ProcessReaper
import matt.shell.context.shell.Bash
import matt.shell.context.shell.Powershell
import matt.shell.context.shell.ShellLanguage
import matt.shell.context.shell.UnixDirectCommandsOnly


interface ShellExecutionContext {

    val language: ShellLanguage

    val shellProgramPathContext: ShellProgramPathContext?

//    val argumentsAreSeparated: Boolean?

    //    val escapeStrategy: Shell
//    val escapeContext: EscapeContext?
    val inSingularityContainer: Boolean?
    val inSlurmJob: Boolean?
    val needsModules: Boolean?
    fun inSingularity(): ShellExecutionContext
    fun inSlurmJob(): ShellExecutionContext
    fun withBash(): ShellExecutionContext
}

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

//    override val argumentsAreSeparated = null

    //    override val escapeContext = null
    override val inSingularityContainer = null
    override val inSlurmJob = null
    override val needsModules = null

    override fun inSingularity() = PartiallyKnownExecutionContext(
        inSingularityContainer = true,
        needsModules = false,
        language = language
    )

    override fun inSlurmJob() = PartiallyKnownExecutionContext(

        shellProgramPathContext = DEFAULT_LINUX_PROGRAM_PATH_CONTEXT,
        inSlurmJob = true,
        language = language

    )

    override fun withBash() = TODO()


}

data class PartiallyKnownExecutionContext(
    override val shellProgramPathContext: ShellProgramPathContext? = null,
//    override val escapeContext: EscapeContext? = null,
//    override val argumentsAreSeparated: Boolean? = null,
    override val inSingularityContainer: Boolean? = null,
    override val inSlurmJob: Boolean? = null,
    override val needsModules: Boolean? = null,
    override val language: ShellLanguage
) : ShellExecutionContext {
    override fun inSingularity(): ShellExecutionContext {
        return copy(
            inSingularityContainer = true,
            needsModules = false
        )
    }

    override fun inSlurmJob() = copy(
        shellProgramPathContext = DEFAULT_LINUX_PROGRAM_PATH_CONTEXT,
        inSlurmJob = true
    )

    override fun withBash() = copy(language = Bash)
}

data class KnownShellExecutionContext(
    override val shellProgramPathContext: ShellProgramPathContext,
//    override val argumentsAreSeparated: Boolean,
//    override val escapeContext: EscapeContext,
    override val inSingularityContainer: Boolean,
    override val inSlurmJob: Boolean,
    override val needsModules: Boolean,
    override val language: ShellLanguage
) : ShellExecutionContext {
    override fun inSingularity(): ShellExecutionContext {
        return copy(
            inSingularityContainer = true,
            needsModules = false
        )
    }

    override fun inSlurmJob() = copy(
        shellProgramPathContext = DEFAULT_LINUX_PROGRAM_PATH_CONTEXT,
        inSlurmJob = true
    )
    override fun withBash() = copy(language = Bash)
}

val DefaultLinuxExecutionContext by lazy {
    PartiallyKnownExecutionContext(
        shellProgramPathContext = DEFAULT_LINUX_PROGRAM_PATH_CONTEXT,
//        escapeContext = NoEscaping,
//        inSingularityContainer = false,
//        inSlurmJob = false,
//        needsModules = false,
        language = UnixDirectCommandsOnly
    )
}

val DefaultMacExecutionContext by lazy {
    KnownShellExecutionContext(
        shellProgramPathContext = DEFAULT_MAC_PROGRAM_PATH_CONTEXT,
//        escapeContext = NoEscaping,
        inSingularityContainer = false,
        inSlurmJob = false,
        needsModules = false,
        language = UnixDirectCommandsOnly
    )
}

val DefaultWindowsExecutionContext by lazy {
    KnownShellExecutionContext(
        shellProgramPathContext = DEFAULT_WINDOWS_PROGRAM_PATH_CONTEXT,
//        escapeContext = NoEscaping,
        inSingularityContainer = false,
        inSlurmJob = false,
        needsModules = false,
        language = Powershell
    )
}


val HasOs.knownShellContextFromOs
    get() = when (os) {
        OsEnum.Linux -> PartiallyKnownExecutionContext(
            shellProgramPathContext = DEFAULT_LINUX_PROGRAM_PATH_CONTEXT,
            language = UnixDirectCommandsOnly
        )

        OsEnum.Mac   -> DefaultMacExecutionContext
        Windows      -> DefaultWindowsExecutionContext
    }