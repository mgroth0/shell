package matt.shell.commands.java

import matt.lang.classname.common.JvmQualifiedClassName
import matt.lang.common.optArray
import matt.lang.model.file.AnyResolvableFilePath
import matt.model.code.jvm.JavaExecArgs
import matt.shell.ControlledShellProgram
import matt.shell.common.Shell

val <R> Shell<R>.java get() = JavaCommand(this)

class JavaCommand<R>(shell: Shell<R>) : ControlledShellProgram<R>(
    shell = shell,
    program = "java"
) {

    fun executeClass(
        jvmArgs: JavaExecArgs,
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
        jvmArgs: JavaExecArgs,
        classpath: List<String>? = null,
        jar: AnyResolvableFilePath,
        commandLineArgs: Array<out String> = arrayOf()
    ) = executeCommon(
        jvmArgs = jvmArgs,
        classpath = classpath,
        executionArgs = arrayOf("-Jar", jar.path),
        commandLineArgs = commandLineArgs
    )


    private fun executeCommon(
        jvmArgs: JavaExecArgs,
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
