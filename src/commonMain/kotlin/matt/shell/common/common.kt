package matt.shell.common

import kotlinx.serialization.Serializable
import matt.lang.anno.Open
import matt.lang.model.file.AnyResolvableFilePath
import matt.log.warn.common.warn
import matt.log.warn.dumpStack
import matt.prim.str.joinWithSpaces
import matt.service.MattService
import matt.shell.common.context.ShellExecutionContext


@DslMarker
annotation class ShellDSL

@ShellDSL
interface Commandable<R> {
    fun sendCommand(vararg args: String): R

    @Open
    fun sendCommand(command: Command): R = sendCommand(*command.commands.toTypedArray())
}

@ShellDSL
interface Shell<R : Any?> : MattService, Commandable<R> {
    @Open
    val AnyResolvableFilePath.pathOp: String get() = path
    val executionContext: ShellExecutionContext
}

@Serializable
/*value class here would be ideal but this failed with https://youtrack.jetbrains.com/issue/KT-57647/Serialization-IllegalAccessError-Update-to-static-final-field-caused-by-serializable-value-class?s=update-to-static-final-field-attempted-from-a-different-method-constructor-impl-than-the-initializer-method-clinit-when


@JvmInline*/
data /*value*/ class Command(val commands: List<String>) {

    fun rawWithNoEscaping() = commands.joinWithSpaces()

    override fun toString(): String {
        warn("don't use vague toString()")
        dumpStack()
        return rawWithNoEscaping()
    }

    fun asArray() = commands.toTypedArray()

    infix fun pipedTo(consumer: Command) = Command(commands + "|" + consumer.commands)

    infix fun pipedToFile(file: AnyResolvableFilePath) = Command(commands + ">" + file.path)

    infix fun and(consumer: Command) = Command(commands + "&&" + consumer.commands)
}
