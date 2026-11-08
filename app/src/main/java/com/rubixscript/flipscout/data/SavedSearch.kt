package com.rubixscript.flipscout.data

import org.json.JSONObject
import java.io.File
import java.util.UUID

/** A search keyword. Pinned ones form the user's keyword library; the rest are just recent searches. */
data class SavedSearch(
    val id: String = UUID.randomUUID().toString(),
    val query: String,
    val pinned: Boolean = false,
    val category: String = "", // blank = uncategorised
    val createdAt: Long = System.currentTimeMillis(),
)

internal fun SavedSearch.toJson(): JSONObject =
    JSONObject().put("id", id).put("q", query).put("pin", pinned).put("cat", category).put("at", createdAt)

internal fun savedSearchFromJson(json: JSONObject) = SavedSearch(
    id = json.getString("id"),
    query = json.getString("q"),
    pinned = json.optBoolean("pin"),
    category = json.optString("cat"),
    createdAt = json.optLong("at"),
)

interface SavedSearchRepository {
    suspend fun load(): List<SavedSearch>
    suspend fun save(searches: List<SavedSearch>)
}

class JsonFileSavedSearchRepository(file: File) : SavedSearchRepository {
    private val store = JsonListStore(file, SavedSearch::toJson, ::savedSearchFromJson)
    override suspend fun load() = store.load()
    override suspend fun save(searches: List<SavedSearch>) = store.save(searches)
}
