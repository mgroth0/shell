package matt.shell.commands.apt.options

import matt.lang.If
import matt.lang.anno.SeeURL
import matt.lang.opt
import matt.shell.commands.apt.options.AptOptions.Companion

class AptOptionsBuilder() {
    internal var current = Companion.DEFAULT
        private set

    var lockTimeoutSeconds: Int?
        get() = current.lockTimeoutSeconds
        set(value) {
            current = current.copy(lockTimeoutSeconds = value)
        }

    var forceConfigStuff: Boolean
        get() = current.forceConfigStuff
        set(value) {
            current = current.copy(forceConfigStuff = value)
        }

}

data class AptOptions(
    val lockTimeoutSeconds: Int? = null,
    val forceConfigStuff: Boolean = false
) {
    companion object {
        val DEFAULT = AptOptions()
    }

    val args
        get() = listOf(
            *opt(lockTimeoutSeconds) {
                @SeeURL("https://unix.stackexchange.com/a/277255/175318")
                "DPkg::Lock::Timeout=$this"
            },
            *If(forceConfigStuff).then("Dpkg::Options::=\"--force-confask,confnew,confmiss\""),
        ).flatMap { listOf("-o", it) }.toTypedArray()
}
