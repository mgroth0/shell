package matt.shell.py


interface PythonEnv<R> {
    fun python(vararg args: String): R
}