package matt.shell.scriptwriter.unix

import matt.lang.inList
import matt.lang.assertions.require.requireIs
import matt.prim.str.joinWithSpaces
import matt.shell.Command
import matt.shell.context.escape.EscapeStrategy
import matt.shell.scriptwriter.ScriptWriter
import matt.shell.scriptwriter.ScriptWriterContext
import matt.shell.scriptwriter.unix.ShExecutor.binBash
import matt.shell.scriptwriter.unix.ShExecutor.usrBinEnv
import matt.shell.scriptwriter.unix.bash.BashWriter

interface UnixWriterContext : ScriptWriterContext {
    fun setVariable(
        name: String,
        value: String
    )

    fun createJob(command: Command)
}

sealed interface Shebang {
    val command: Command
}


internal class EnvShebang(override val command: Command) : Shebang

enum class ShExecutor(val path: String) : Shebang {
    binBash("/bin/bash"),
    usrBinEnv("/usr/bin/env");

    override val command = Command(path.inList())
    fun withArgs(vararg args: String): Shebang = EnvShebang(Command(this@ShExecutor.command.commands + args.toList()))
}


abstract class UnixShell(
    escapeStrategy: EscapeStrategy,
) : ScriptWriter(escapeStrategy), UnixWriterContext {

    private var wroteShebang = false

    @Synchronized
    fun shebang(loader: Shebang) {
        when (loader) {
            is ShExecutor -> when (loader) {
                binBash   -> requireIs<BashWriter>(this)
                usrBinEnv -> error("I think this requires args")
            }

            is EnvShebang -> Unit
        }
        if (wroteShebang) {
            error("already wrote shebang")
        }
        wroteShebang = true
        addRawLines("#!${loader.command.commands.joinWithSpaces()}", index = 0)
    }

    override fun setVariable(
        name: String,
        value: String
    ) {
        addRawLines("$name=$value")
    }

    override fun createJob(command: Command) {
        addRawLines(command.rawWithNoEscaping() + " &")
    }

}