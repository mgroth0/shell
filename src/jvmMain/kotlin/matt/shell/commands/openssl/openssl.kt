package matt.shell.commands.openssl

import matt.lang.model.file.AnyFsFile
import matt.shell.ControlledShellProgram
import matt.shell.common.Shell


val <R> Shell<R>.openSsl get() = OpenSslCommand(this)

class OpenSslCommand<R>(shell: Shell<R>) : ControlledShellProgram<R>(
    shell = shell,
    program = "openssl"
) {

    companion object {
        private const val PRIV_KEY_SIZE = 2048
    }

    fun generatePrivateKey(
        outputKeyFile: AnyFsFile
    ) = sendCommand("genrsa", "-out", outputKeyFile.path, PRIV_KEY_SIZE.toString())

    fun generateModernPrivateKey(
        outputKeyFile: AnyFsFile
    ) = sendCommand(
        "genpkey",
        "-algorithm",
        "RSA",
        "-out",
        outputKeyFile.path,
        "-pkeyopt",
        "rsa_keygen_bits:$PRIV_KEY_SIZE"
    )

    fun createCertificateSigningRequest(
        inputKeyFile: AnyFsFile,
        outputCsrFile: AnyFsFile,
        ipAddress: String
    ) = sendCommand(
        "req",
        "-new",
        "-key",
        inputKeyFile.path,
        "-out",
        outputCsrFile.path,
        "-subj",
        "/CN=matt" /*including -subj to avoid this command from prompting for user input*/,
        "-addext",
        "subjectAltName = IP:$ipAddress"
    )

    /*
    ChatGPT says that it is basically unheard of to create a certificate without an expiration date, and that while I could make one that expires in 100 years many systems do not allow expirations have 2038 due to the 2038 problem.
     * */
    fun generateSelfSignedCertificate(
        inputCsrFile: AnyFsFile,
        inputKeyFile: AnyFsFile,
        outputCertFile: AnyFsFile
    ) = sendCommand(
        "x509",
        "-req",
        "-days",
        "2", /*temporary, so I can be prompted in a couple days to create an appropriate certificate cycling system*/
        "-in",
        inputCsrFile.path,
        "-signkey",
        inputKeyFile.path,
        "-out",
        outputCertFile.path
    )
}

