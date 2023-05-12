package matt.shell.commands.java

import matt.model.code.jvm.JvmArgs
import matt.shell.Shell

fun <R> Shell<R>.java(
  jvmArgs: JvmArgs,
  vararg args: String
): R = sendCommand(
  this::java.name,
  *jvmArgs.args,
  *args
)