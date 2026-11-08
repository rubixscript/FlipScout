package com.rubixscript.flipscout.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Persists a list as a JSON array in [file], using write-to-temp-then-rename so a crash never corrupts data. */
internal class JsonListStore<T>(
    private val file: File,
    private val encode: (T) -> JSONObject,
    private val decode: (JSONObject) -> T,
) {
    suspend fun load(): List<T> = withContext(Dispatchers.IO) {
        runCatching {
            val arr = JSONArray(file.readText())
            List(arr.length()) { decode(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    suspend fun save(items: List<T>) = withContext(Dispatchers.IO) {
        val tmp = File(file.path + ".tmp")
        tmp.writeText(JSONArray(items.map(encode)).toString())
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
        Unit
    }
}
