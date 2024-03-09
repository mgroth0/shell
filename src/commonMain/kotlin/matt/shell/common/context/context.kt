package matt.shell.common.context

import matt.lang.context.DEFAULT_LINUX_PROGRAM_PATH_CONTEXT
import matt.lang.context.DEFAULT_MAC_PROGRAM_PATH_CONTEXT
import matt.lang.context.ShellProgramPathContext
import matt.shell.common.context.shell.Bash
import matt.shell.common.context.shell.ShellLanguage
import matt.shell.common.context.shell.UnixDirectCommandsOnly

interface ShellExecutionContext {

    val language: ShellLanguage

    val shellProgramPathContext: ShellProgramPathContext?

    val inSingularityContainer: Boolean?
    val inSlurmJob: Boolean?
    val needsModules: Boolean?
    fun inSingularity(): ShellExecutionContext
    fun inSlurmJob(): ShellExecutionContext
    fun withBash(): ShellExecutionContext
}


data class KnownShellExecutionContext(
    override val shellProgramPathContext: ShellProgramPathContext,
    override val inSingularityContainer: Boolean,
    override val inSlurmJob: Boolean,
    override val needsModules: Boolean,
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

val DefaultMacExecutionContext by lazy {
    KnownShellExecutionContext(
        shellProgramPathContext = DEFAULT_MAC_PROGRAM_PATH_CONTEXT,
        inSingularityContainer = false,
        inSlurmJob = false,
        needsModules = false,
        language = UnixDirectCommandsOnly
    )
}
