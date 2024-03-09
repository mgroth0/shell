package matt.shell.dispatch

import matt.shell.common.Shell

interface ShellScriptDispatcher<SH : Shell<*>> {
    fun dispatch(script: SH.() -> Unit)
}



class ExecuteInPlace<SH : Shell<*>>(private val shell: SH) : ShellScriptDispatcher<SH> {
    override fun dispatch(script: SH.() -> Unit) {
        shell.script()
    }
}

