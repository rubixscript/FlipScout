package com.rubixscript.flipscout.domain

/**
 * Minimal JSON reader that returns plain Kotlin values: Map<String, Any?>, List<Any?>, String, or null for
 * numbers/booleans/null (callers here only care about text). Pure Kotlin so it runs in JVM unit tests, unlike
 * android.jar's org.json stubs. Reads the first value and ignores whatever follows it.
 */
internal fun readFirstJsonValue(text: String): Any? = runCatching { JsonReader(text).readValue() }.getOrNull()

private class JsonReader(private val s: String) {
    private var i = 0

    fun readValue(): Any? {
        skipWhitespace()
        return when (peek()) {
            '{' -> readObject()
            '[' -> readArray()
            '"' -> readString()
            else -> readLiteral()
        }
    }

    private fun readObject(): Map<String, Any?> {
        expect('{')
        val map = LinkedHashMap<String, Any?>()
        skipWhitespace()
        if (peek() == '}') { i++; return map }
        while (true) {
            skipWhitespace()
            val key = readString()
            skipWhitespace(); expect(':')
            map[key] = readValue()
            skipWhitespace()
            when (next()) { ',' -> continue; '}' -> return map; else -> fail() }
        }
    }

    private fun readArray(): List<Any?> {
        expect('[')
        val list = ArrayList<Any?>()
        skipWhitespace()
        if (peek() == ']') { i++; return list }
        while (true) {
            list += readValue()
            skipWhitespace()
            when (next()) { ',' -> continue; ']' -> return list; else -> fail() }
        }
    }

    private fun readString(): String {
        expect('"')
        val sb = StringBuilder()
        while (true) {
            when (val c = next()) {
                '"' -> return sb.toString()
                '\\' -> when (val e = next()) {
                    'n' -> sb.append('\n'); 't' -> sb.append('\t'); 'r' -> sb.append('\r')
                    'b' -> sb.append('\b'); 'f' -> sb.append('\u000C')
                    'u' -> { sb.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                    else -> sb.append(e) // \" \\ \/
                }
                else -> sb.append(c)
            }
        }
    }

    private fun readLiteral(): Any? {
        val start = i
        while (i < s.length && s[i] !in ",]} \t\r\n") i++
        if (i == start) fail()
        return null
    }

    private fun skipWhitespace() { while (i < s.length && s[i].isWhitespace()) i++ }
    private fun peek(): Char = if (i < s.length) s[i] else fail()
    private fun next(): Char = if (i < s.length) s[i++] else fail()
    private fun expect(c: Char) { if (next() != c) fail() }
    private fun fail(): Nothing = throw IllegalArgumentException("Invalid JSON at $i")
}
