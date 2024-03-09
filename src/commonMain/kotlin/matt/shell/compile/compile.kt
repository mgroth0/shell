package matt.shell.compile


internal fun compileSingleCharArguments(vararg args: String): Array<String> {
    val r = mutableListOf<String>()
    val toCompile = mutableListOf<Char>()
    fun compileLast() {
        if (toCompile.isNotEmpty()) {
            r += "-${toCompile.joinToString(separator = "") { it.toString() }}"
            toCompile.clear()
        }
    }
    args.forEach {
        if (it.length != 2 || it.first() != '-') {
            compileLast()
            r += it
            return@forEach
        }
        val letter = it[1]
        toCompile.add(letter)
    }
    compileLast()
    return r.toTypedArray()
}
