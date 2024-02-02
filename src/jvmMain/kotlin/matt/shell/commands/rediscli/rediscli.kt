package matt.shell.commands.rediscli

import matt.collect.itr.flatMapToArray
import matt.lang.model.file.AnyFsFile
import matt.lang.optArray
import matt.model.redis.RedisConfigurationPatch
import matt.shell.ControlledShellProgram
import matt.shell.Shell

private val DEFAULT_PW = null
private val DEFAULT_TLS = null

data class TlsConfig(
    val certificate: AnyFsFile,
    val privateKey: AnyFsFile,
    val certificateAuthorityCertificate: AnyFsFile
)

fun <R : Any> Shell<R>.redisCli(
    password: String? = DEFAULT_PW,
    tls: TlsConfig? = DEFAULT_TLS,
    op: RedisCliCommand<R>.() -> Unit = {}
) = RedisCliCommand(this, password = password, tls = tls).apply(op)

class RedisCliCommand<R : Any>(
    shell: Shell<R>,
    private val password: String? = DEFAULT_PW,
    private val tls: TlsConfig? = DEFAULT_TLS
) : ControlledShellProgram<R>(
        shell = shell,
        program = "redis-cli"
    ) {


    fun configSet(
        patch: RedisConfigurationPatch
    ): R {
        val patchProps = patch.nonNullProperties()
        require(patchProps.isNotEmpty()) {
            "CONFIG SET fails if there are no properties set"
        }
        return redisCommand("CONFIG", "SET", *patchProps.entries.flatMapToArray { listOf(it.key, it.value) })
    }


    fun configRewrite() = redisCommand("CONFIG", "REWRITE")


    private fun redisCommand(
        vararg command: String
    ) = sendCommand(
        "-e" /*critically needed, or else it could silently fail*/,
        *optArray(password) { arrayOf("-a", this) },
        *optArray(tls) {
            arrayOf(
                "--tls",
                "--key",
                privateKey.path,
                "--cert",
                certificate.path,
                "--cacert",
                certificateAuthorityCertificate.path
            )
        },
        * command
    )


}


