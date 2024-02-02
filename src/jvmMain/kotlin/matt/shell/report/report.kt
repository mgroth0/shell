package matt.shell.report

import matt.lang.model.file.AnyResolvableFilePath
import matt.log.textart.TEXT_BAR
import matt.model.code.errreport.Report
import matt.prim.str.elementsToString
import matt.prim.str.joinWithSpaces
import matt.prim.str.mybuild.api.string

class ShellErrorReport(
    workingDir: AnyResolvableFilePath?,
    env: Map<String, String>,
    args: Array<out String>,
    result: ShellResult
) : Report() {
    override val text by lazy {
        string {
            lineDelimited {
                +"Shell Error"
                blankLine()
                +"Working Dir: $workingDir"
                blankLine()
                +"Env:"
                +TEXT_BAR
                +env.entries.joinToString("\n") { "\"${it.key}\": \"${it.value}\"" }
                +TEXT_BAR
                blankLine()
                +"Full Command:"
                +TEXT_BAR
                +args.elementsToString()
                +TEXT_BAR
                blankLine()
                +"Full Command (copyable):"
                +TEXT_BAR
                +args.joinWithSpaces()
                +TEXT_BAR
                blankLine()
                +"Error Code: ${result.code}"
                blankLine()
                if (result is ShellFullResult) {
                    +"Full Std Out:"
                    +TEXT_BAR
                    +result.std
                    +TEXT_BAR
                    blankLine()
                    +"Full Std Err:"
                    +TEXT_BAR
                    +result.err
                    +TEXT_BAR
                } else {
                    +"shell result did not include output"
                }
            }
        }
    }

}

class NonZeroShellResult(
    result: ShellResult,
    report: ShellErrorReport
) : Exception(report.text) {
    val code = result.code
    val output = (result as? ShellFullResult)?.output
}

open class ShellResult(
    val code: Int,
)

class ShellFullResult(
    code: Int,
    internal val std: String,
    internal val err: String
) : ShellResult(code) {
    val output = std + err
}

class ShellErrException(m: String) : Exception(m)
