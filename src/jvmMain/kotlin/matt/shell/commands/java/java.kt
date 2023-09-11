package matt.shell.commands.java

import matt.lang.classname.JvmQualifiedClassName
import matt.lang.model.file.FilePath
import matt.lang.optArray
import matt.model.code.jvm.JvmArgs
import matt.shell.ControlledShellProgram
import matt.shell.Shell

val <R> Shell<R>.java get() = JavaCommand(this)

class JavaCommand<R>(shell: Shell<R>) : ControlledShellProgram<R>(
    shell = shell,
    program = "java"
) {

    fun executeClass(
        jvmArgs: JvmArgs,
        classpath: List<String>? = null,
        mainClass: JvmQualifiedClassName,
        commandLineArgs: Array<out String> = arrayOf()
    ) = executeCommon(
        jvmArgs = jvmArgs,
        classpath = classpath,
        executionArgs = arrayOf(mainClass.name),
        commandLineArgs = commandLineArgs
    )

    fun executeJar(
        jvmArgs: JvmArgs,
        classpath: List<String>? = null,
        jar: FilePath,
        commandLineArgs: Array<out String> = arrayOf()
    ) = executeCommon(
        jvmArgs = jvmArgs,
        classpath = classpath,
        executionArgs = arrayOf("-Jar", jar.filePath),
        commandLineArgs = commandLineArgs
    )


    private fun executeCommon(
        jvmArgs: JvmArgs,
        classpath: List<String>? = null,
        executionArgs: Array<String>,
        commandLineArgs: Array<out String>
    ) = sendCommand(
        *jvmArgs.args,
        *optArray(classpath) {
            arrayOf(
                "-classpath",
                joinToString(separator = ":")
            )
        },
        *executionArgs,
        *commandLineArgs
    )

}