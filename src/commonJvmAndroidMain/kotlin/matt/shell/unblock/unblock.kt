package matt.shell.unblock

import kotlinx.coroutines.delay
import matt.lang.unblock.DEFAULT_UNBLOCK_DELAY


suspend fun Process.waitForSuspending(): Int {
    while (isAlive) {
        delay(DEFAULT_UNBLOCK_DELAY)
    }
    return exitValue()
}
