package matt.shell.context

import matt.lang.context.DEFAULT_LINUX_PROGRAM_PATH_CONTEXT
import matt.lang.context.DEFAULT_MAC_PROGRAM_PATH_CONTEXT
import matt.lang.context.DEFAULT_WINDOWS_PROGRAM_PATH_CONTEXT
import matt.lang.context.ShellProgramPathContext

interface ShellExecutionContext {
    val shellProgramPathContext: ShellProgramPathContext?

//    val argumentsAreSeparated: Boolean?

    //    val escapeStrategy: Shell
//    val escapeContext: EscapeContext?
    val inSingularityContainer: Boolean?
    val inSlurmJob: Boolean?
    val needsModules: Boolean?
    fun inSingularity(): ShellExecutionContext
    fun inSlurmJob(): ShellExecutionContext
}

object UnknownShellExecutionContext : ShellExecutionContext {
    override val shellProgramPathContext = null

//    override val argumentsAreSeparated = null

    //    override val escapeContext = null
    override val inSingularityContainer = null
    override val inSlurmJob = null
    override val needsModules = null

    override fun inSingularity() = PartiallyKnownExecutionContext(
        inSingularityContainer = true,
        needsModules = false
    )

    override fun inSlurmJob() = PartiallyKnownExecutionContext(

        shellProgramPathContext = DEFAULT_LINUX_PROGRAM_PATH_CONTEXT,
        inSlurmJob = true

    )

}

data class PartiallyKnownExecutionContext(
    override val shellProgramPathContext: ShellProgramPathContext? = null,
//    override val escapeContext: EscapeContext? = null,
//    override val argumentsAreSeparated: Boolean? = null,
    override val inSingularityContainer: Boolean? = null,
    override val inSlurmJob: Boolean? = null,
    override val needsModules: Boolean? = null,
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
}

data class KnownShellExecutionContext(
    override val shellProgramPathContext: ShellProgramPathContext,
//    override val argumentsAreSeparated: Boolean,
//    override val escapeContext: EscapeContext,
    override val inSingularityContainer: Boolean,
    override val inSlurmJob: Boolean,
    override val needsModules: Boolean
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
}

val DefaultLinuxExecutionContext by lazy {
    PartiallyKnownExecutionContext(
        shellProgramPathContext = DEFAULT_LINUX_PROGRAM_PATH_CONTEXT,
//        escapeContext = NoEscaping,
//        inSingularityContainer = false,
//        inSlurmJob = false,
//        needsModules = false
    )
}

val DefaultMacExecutionContext by lazy {
    KnownShellExecutionContext(
        shellProgramPathContext = DEFAULT_MAC_PROGRAM_PATH_CONTEXT,
//        escapeContext = NoEscaping,
        inSingularityContainer = false,
        inSlurmJob = false,
        needsModules = false,
    )
}

val DefaultWindowsExecutionContext by lazy {
    KnownShellExecutionContext(
        shellProgramPathContext = DEFAULT_WINDOWS_PROGRAM_PATH_CONTEXT,
//        escapeContext = NoEscaping,
        inSingularityContainer = false,
        inSlurmJob = false,
        needsModules = false,
    )
}