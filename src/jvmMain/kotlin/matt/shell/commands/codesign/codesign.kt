package matt.shell.commands.codesign

import matt.collect.itr.mapToArray
import matt.lang.common.If
import matt.lang.common.opt
import matt.lang.common.optArray
import matt.model.data.id.CodesignIdentity
import matt.model.data.message.AbsMacFile
import matt.shell.common.Shell



fun <R> Shell<R>.codesign(
    identity: CodesignIdentity? = null,
    verbosity: Int? = null,
    force: Boolean = false /*replace existing signatures (otherwise it would fail if other signatures exist)*/,
    entitlements: AbsMacFile? = null,
    executable: AbsMacFile,
    /*makes the code signature more trustworthy, especially in the long term when a certificate might expire*/
    timestamp: Boolean? = null,
    options: Set<CodesignOption> = setOf(),
    prefix: String? = null,
    display: Boolean = false
): R {
    require(verbosity in 1..4) {
        "I think verbosity has to be from 1 to 4 but I cannot find a straight answer"
    }
    val opts = options.toSet()
    return sendCommand(
        CODESIGN_PATH,
        *optArray(identity) { arrayOf("-s", arg) },
        /*
        (HORRIBLE DESIGN NOTE)
         `-v` could mean VERIFY not VERBOSE in different contexts
        So use --verbose instead so there is 0 ambiguity
         */
        *opt(verbosity) { "--verbose=$this" },
        *If(force).then("-f"),
        *optArray(entitlements) { arrayOf("--entitlements", path) },
        *optArray(timestamp) {
            arrayOf("--timestamp", *If(!this).then("none"))
        },
        *If(opts.isNotEmpty()).then("--options", *opts.mapToArray { it.name }),
        *optArray(prefix) { arrayOf("--prefix", this) },
        *If(display).then("-d"),
        executable.path
    )
}



const val CODESIGN_PATH = "/usr/bin/codesign"


enum class CodesignOption {
    /*required for notarizing (and thus for distributing outside the Mac App Store)*/
    runtime
}
