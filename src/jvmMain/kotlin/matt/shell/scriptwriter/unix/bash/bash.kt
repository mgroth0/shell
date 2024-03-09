package matt.shell.scriptwriter.unix.bash

import matt.lang.anno.SeeURL
import matt.shell.common.context.ShellExecutionContext
import matt.shell.common.context.shell.Bash
import matt.shell.commonj.context.escape.EscapeStrategy
import matt.shell.scriptwriter.unix.UnixShell
import matt.shell.scriptwriter.unix.UnixWriterContext

const val SANE_BASH_CONFIG_DEFAULT = true

interface BashWriterContext : UnixWriterContext {
    fun `while`(
        condition: String,
        op: BashLoopContext.() -> Unit
    )

    fun `for`(
        args: String,
        op: BashLoopContext.() -> Unit
    )

    fun `if`(
        condition: String,
        op: BashWriterContext.() -> Unit
    )

    fun ifElif(
        ifCondition: String,
        `if`: BashWriterContext.() -> Unit,
        elseCondition: String,
        `else`: BashWriterContext.() -> Unit
    )

    fun ifElse(
        ifCondition: String,
        `if`: BashWriterContext.() -> Unit,
        `else`: BashWriterContext.() -> Unit
    )
}


class BashWriter(
    override val executionContext: ShellExecutionContext,
    escapeStrategy: EscapeStrategy,
    @SeeURL("https://stackoverflow.com/questions/821396/aborting-a-shell-script-if-any-command-returns-a-non-zero-value")
    private val saneBashConfig: Boolean = SANE_BASH_CONFIG_DEFAULT
) : UnixShell(
        escapeStrategy = escapeStrategy
    ),
    BashWriterContext {

    fun setSaneConfig() = addRawLines("set -Eueo pipefail")

    init {
        check(executionContext.language == Bash)
        if (saneBashConfig) {
            @SeeURL("https://stackoverflow.com/a/821419/6596010") setSaneConfig()
        }
    }


    override fun `while`(
        condition: String,
        op: BashLoopContext.() -> Unit
    ) {
        addRawLines("while $condition")
        addRawLines("do")
        BashLoopContext(this).apply(op)
        addRawLines("done")
    }

    override fun `if`(
        condition: String,
        op: BashWriterContext.() -> Unit
    ) {
        addRawLines("if $condition; then")
        BashLoopContext(this).apply(op)
        addRawLines("fi")
    }

    override fun ifElif(
        ifCondition: String,
        `if`: BashWriterContext.() -> Unit,
        elseCondition: String,
        `else`: BashWriterContext.() -> Unit
    ) {
        addRawLines("if $ifCondition; then")
        BashLoopContext(this).apply(`if`)
        addRawLines("elif $elseCondition; then")
        BashLoopContext(this).apply(`else`)
        addRawLines("fi")
    }

    override fun ifElse(
        ifCondition: String,
        `if`: BashWriterContext.() -> Unit,
        `else`: BashWriterContext.() -> Unit
    ) {
        addRawLines("if $ifCondition; then")
        BashLoopContext(this).apply(`if`)
        addRawLines("else")
        BashLoopContext(this).apply(`else`)
        addRawLines("fi")
    }

    override fun `for`(
        args: String,
        op: BashLoopContext.() -> Unit
    ) {
        addRawLines("for $args; do")
        BashLoopContext(this).apply(op)
        addRawLines("done")
    }
}


class BashLoopContext(writer: BashWriter) : BashWriterContext by writer {
    fun `break`() = addRawLines("break")
}
