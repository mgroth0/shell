package matt.shell.ansi

import matt.log.j.NOPLogger
import matt.log.j.SystemOutLogger
import matt.log.logger.Logger
import matt.stream.ansi.AnsiColor


interface ColorableLogger : Logger {
    fun print(
        a: Any,
        color: AnsiColor
    )

    fun println(
        a: Any,
        color: AnsiColor
    )
}

class ColorableLoggerImpl(innerLogger: Logger) : ColorableLogger, Logger by innerLogger {
    override fun print(
        a: Any,
        color: AnsiColor
    ) {
        print(color.wrap(a.toString()))
    }

    override fun println(
        a: Any,
        color: AnsiColor
    ) {
        println(color.wrap(a.toString()))
    }
}


val SystemOutAnsiLogger = ColorableLoggerImpl(SystemOutLogger)
val NopAnsiLogger = ColorableLoggerImpl(NOPLogger)
